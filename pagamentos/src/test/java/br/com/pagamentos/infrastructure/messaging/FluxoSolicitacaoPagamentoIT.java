package br.com.pagamentos.infrastructure.messaging;

import br.com.pagamentos.application.port.out.Decisoes;
import br.com.pagamentos.application.exception.FalhaPublicacaoException;
import br.com.pagamentos.domain.CenarioPagamento;
import br.com.pagamentos.domain.DecisaoPagamento;
import br.com.pagamentos.infrastructure.persistence.SpringDataDecisaoPagamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static br.com.pagamentos.infrastructure.messaging.PagamentosMensageria.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Usa o RabbitMQ e o Postgres do Compose. A API de Pedidos precisa estar parada: ela consome
 * pedidos.resultados e trataria estes resultados de teste como desconhecidos. Não purga filas: cada teste
 * consome apenas as mensagens com o seu messageId.
 */
@SpringBootTest
class FluxoSolicitacaoPagamentoIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private Decisoes decisoes;
    @Autowired
    private SpringDataDecisaoPagamentoRepository repositorio;

    private void enviar(UUID solicitacaoId, UUID pedidoId, String valor, String cenario, Integer versao) {
        var corpo = new SolicitacaoPagamentoMensagem(solicitacaoId, pedidoId, new BigDecimal(valor), cenario);
        rabbitTemplate.convertAndSend(EXCHANGE_COMANDOS, CHAVE_SOLICITAR, corpo, mp -> {
            if (versao != null) {
                mp.getMessageProperties().setHeader(HEADER_VERSAO, versao);
            }
            mp.getMessageProperties().setMessageId(solicitacaoId.toString());
            return mp;
        });
    }

    /** Espera (até 10 s) uma mensagem da fila com o messageId; ignora (sem consumir de volta) as demais. */
    private Message esperar(String fila, UUID solicitacaoId) {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            Message m = rabbitTemplate.receive(fila, 500);
            if (m == null) {
                continue;
            }
            if (solicitacaoId.toString().equals(m.getMessageProperties().getMessageId())) {
                return m;
            }
            rabbitTemplate.send(fila, m);
        }
        return null;
    }

    private ResultadoPagamentoMensagem resultado(Message m) {
        var conversor = (SmartMessageConverter) rabbitTemplate.getMessageConverter();
        return (ResultadoPagamentoMensagem) conversor.fromMessage(m,
                new ParameterizedTypeReference<ResultadoPagamentoMensagem>() { });
    }

    @Test
    @DisplayName("Solicitação válida: decisão salva e resultado persistente em pedidos.resultados, com versao=1")
    void aprova() {
        var sol = UUID.randomUUID();
        var ped = UUID.randomUUID();

        enviar(sol, ped, "37.80", "APROVAR", 1);

        Message m = esperar(FILA_RESULTADOS, sol);
        assertNotNull(m, "resultado não apareceu em pedidos.resultados");
        assertEquals(MessageDeliveryMode.PERSISTENT, m.getMessageProperties().getReceivedDeliveryMode());
        assertEquals(1, ((Number) m.getMessageProperties().getHeader(HEADER_VERSAO)).intValue());
        var r = resultado(m);
        assertEquals(sol, r.solicitacaoId());
        assertEquals(ped, r.pedidoId());
        assertEquals(0, new BigDecimal("37.80").compareTo(r.valor()));
        assertTrue(r.aprovado());
        assertEquals("APROVADO_SIMULADO", r.motivo());
        assertTrue(repositorio.existsById(sol), "decisão não persistida");
    }

    @Test
    @DisplayName("RECUSAR gera aprovado=false")
    void recusa() {
        var sol = UUID.randomUUID();
        enviar(sol, UUID.randomUUID(), "10.00", "RECUSAR", 1);

        var r = resultado(esperar(FILA_RESULTADOS, sol));

        assertFalse(r.aprovado());
        assertEquals("RECUSADO_SIMULADO", r.motivo());
    }

    @Test
    @DisplayName("Recebimento repetido: uma só decisão e o mesmo resultado republicado")
    void repetido() {
        var sol = UUID.randomUUID();
        var ped = UUID.randomUUID();

        enviar(sol, ped, "37.80", "APROVAR", 1);
        var primeiro = resultado(esperar(FILA_RESULTADOS, sol));
        enviar(sol, ped, "37.80", "APROVAR", 1);
        var segundo = resultado(esperar(FILA_RESULTADOS, sol));

        assertEquals(primeiro, segundo);
        assertEquals(1, repositorio.findAll().stream().filter(e -> e.getId().equals(sol)).count());
    }

    @Test
    @DisplayName("Mesma chave com outro valor: sem novo resultado, mensagem vai para a fila de erros")
    void conflito() {
        var sol = UUID.randomUUID();
        var ped = UUID.randomUUID();
        enviar(sol, ped, "37.80", "APROVAR", 1);
        assertNotNull(esperar(FILA_RESULTADOS, sol));

        enviar(sol, ped, "99.00", "APROVAR", 1);

        assertNotNull(esperar(FILA_SOLICITACOES_ERROS, sol), "conflito deveria ir para pagamentos.solicitacoes.erros");
        var salva = decisoes.porId(sol).orElseThrow();
        assertEquals(0, new BigDecimal("37.80").compareTo(salva.valor()));
    }

    @Test
    @DisplayName("Sem header de versão: vai para a fila de erros e não grava decisão")
    void semVersao() {
        var sol = UUID.randomUUID();
        enviar(sol, UUID.randomUUID(), "37.80", "APROVAR", null);

        assertNotNull(esperar(FILA_SOLICITACOES_ERROS, sol));
        assertFalse(repositorio.existsById(sol));
    }

    @Test
    @DisplayName("Versão desconhecida: vai para a fila de erros e não grava decisão")
    void versaoDesconhecida() {
        var sol = UUID.randomUUID();
        enviar(sol, UUID.randomUUID(), "37.80", "APROVAR", 2);

        assertNotNull(esperar(FILA_SOLICITACOES_ERROS, sol));
        assertFalse(repositorio.existsById(sol));
    }

    @Test
    @DisplayName("Valor negativo e cenário desconhecido: fila de erros, sem decisão")
    void invalidas() {
        var negativo = UUID.randomUUID();
        var cenario = UUID.randomUUID();
        enviar(negativo, UUID.randomUUID(), "-1.00", "APROVAR", 1);
        enviar(cenario, UUID.randomUUID(), "5.00", "TALVEZ", 1);

        assertNotNull(esperar(FILA_SOLICITACOES_ERROS, negativo));
        assertNotNull(esperar(FILA_SOLICITACOES_ERROS, cenario));
        assertFalse(repositorio.existsById(negativo));
        assertFalse(repositorio.existsById(cenario));
    }

    @Test
    @DisplayName("Publicação sem rota é detectada (return) e vira FalhaPublicacaoException")
    void semRota() {
        var publicador = new PublicadorResultadoAmqp(rabbitTemplate, EXCHANGE_EVENTOS, "chave.inexistente",
                Duration.ofSeconds(5));
        var d = DecisaoPagamento.decidir(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"),
                CenarioPagamento.APROVAR);

        assertThrows(FalhaPublicacaoException.class, () -> publicador.publicar(d));
    }

    @Test
    @DisplayName("Exchange inexistente é detectado (nack) e vira FalhaPublicacaoException")
    void exchangeInexistente() {
        var publicador = new PublicadorResultadoAmqp(rabbitTemplate, "exchange.inexistente", CHAVE_RESULTADO,
                Duration.ofSeconds(5));
        var d = DecisaoPagamento.decidir(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"),
                CenarioPagamento.APROVAR);

        assertThrows(FalhaPublicacaoException.class, () -> publicador.publicar(d));
    }
}
