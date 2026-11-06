package br.com.pagamentos.application.service;

import br.com.pagamentos.application.exception.DecisaoConflitanteException;
import br.com.pagamentos.application.exception.FalhaPublicacaoException;
import br.com.pagamentos.application.exception.SolicitacaoInvalidaException;
import br.com.pagamentos.application.port.out.Decisoes;
import br.com.pagamentos.application.port.out.PublicadorResultado;
import br.com.pagamentos.domain.DecisaoPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessarSolicitacaoPagamentoServiceTest {

    static class DecisoesFake implements Decisoes {
        final Map<UUID, DecisaoPagamento> dados = new HashMap<>();
        final List<String> eventos;

        DecisoesFake(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public Optional<DecisaoPagamento> porId(UUID id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public DecisaoPagamento salvarSeAusente(DecisaoPagamento d) {
            eventos.add("salvou");
            return dados.computeIfAbsent(d.solicitacaoId(), k -> d);
        }
    }

    static class PublicadorFake implements PublicadorResultado {
        final List<DecisaoPagamento> publicadas = new ArrayList<>();
        final List<String> eventos;
        boolean falhar;

        PublicadorFake(List<String> eventos) {
            this.eventos = eventos;
        }

        @Override
        public void publicar(DecisaoPagamento d) {
            eventos.add("publicou");
            if (falhar) {
                throw new FalhaPublicacaoException("sem confirmação");
            }
            publicadas.add(d);
        }
    }

    private final UUID solicitacao = UUID.randomUUID();
    private final UUID pedido = UUID.randomUUID();
    private final BigDecimal valor = new BigDecimal("37.80");
    private List<String> eventos;
    private DecisoesFake decisoes;
    private PublicadorFake publicador;
    private ProcessarSolicitacaoPagamentoService service;

    @BeforeEach
    void preparar() {
        eventos = new ArrayList<>();
        decisoes = new DecisoesFake(eventos);
        publicador = new PublicadorFake(eventos);
        service = new ProcessarSolicitacaoPagamentoService(decisoes, publicador);
    }

    @Test
    @DisplayName("Salva a decisão antes de publicar e preserva IDs e valor")
    void salvaDepoisPublica() {
        service.executar(solicitacao, pedido, valor, "APROVAR");

        assertEquals(List.of("salvou", "publicou"), eventos);
        var publicada = publicador.publicadas.get(0);
        assertEquals(solicitacao, publicada.solicitacaoId());
        assertEquals(pedido, publicada.pedidoId());
        assertEquals(valor, publicada.valor());
        assertTrue(publicada.aprovado());
    }

    @Test
    @DisplayName("RECUSAR publica aprovado=false")
    void recusa() {
        service.executar(solicitacao, pedido, valor, "RECUSAR");

        assertFalse(publicador.publicadas.get(0).aprovado());
    }

    @Test
    @DisplayName("Reentrega: reutiliza a decisão salva e republica o mesmo resultado")
    void reentrega() {
        service.executar(solicitacao, pedido, valor, "APROVAR");
        service.executar(solicitacao, pedido, new BigDecimal("37.8"), "APROVAR");

        assertEquals(1, decisoes.dados.size());
        assertEquals(2, publicador.publicadas.size());
        assertEquals(publicador.publicadas.get(0), publicador.publicadas.get(1));
        assertEquals(1, eventos.stream().filter("salvou"::equals).count());
    }

    @Test
    @DisplayName("Mesma chave com outro pedido, valor ou cenário falha sem publicar")
    void conflito() {
        service.executar(solicitacao, pedido, valor, "APROVAR");
        publicador.publicadas.clear();

        assertThrows(DecisaoConflitanteException.class,
                () -> service.executar(solicitacao, UUID.randomUUID(), valor, "APROVAR"));
        assertThrows(DecisaoConflitanteException.class,
                () -> service.executar(solicitacao, pedido, new BigDecimal("10.00"), "APROVAR"));
        assertThrows(DecisaoConflitanteException.class,
                () -> service.executar(solicitacao, pedido, valor, "RECUSAR"));
        assertTrue(publicador.publicadas.isEmpty());
    }

    @Test
    @DisplayName("Falha de publicação mantém a decisão salva e propaga a exceção")
    void falhaDePublicacao() {
        publicador.falhar = true;

        assertThrows(FalhaPublicacaoException.class, () -> service.executar(solicitacao, pedido, valor, "APROVAR"));

        assertTrue(decisoes.dados.containsKey(solicitacao));
        publicador.falhar = false;
        service.executar(solicitacao, pedido, valor, "APROVAR");
        assertEquals(1, publicador.publicadas.size());
    }

    @Test
    @DisplayName("Entradas inválidas não salvam nem publicam")
    void invalidas() {
        assertThrows(SolicitacaoInvalidaException.class, () -> service.executar(null, pedido, valor, "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class, () -> service.executar(solicitacao, null, valor, "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class, () -> service.executar(solicitacao, pedido, null, "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class,
                () -> service.executar(solicitacao, pedido, BigDecimal.ZERO, "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class,
                () -> service.executar(solicitacao, pedido, new BigDecimal("-5"), "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class,
                () -> service.executar(solicitacao, pedido, new BigDecimal("1.005"), "APROVAR"));
        assertThrows(SolicitacaoInvalidaException.class, () -> service.executar(solicitacao, pedido, valor, null));
        assertThrows(SolicitacaoInvalidaException.class, () -> service.executar(solicitacao, pedido, valor, "TALVEZ"));

        assertTrue(eventos.isEmpty());
    }
}
