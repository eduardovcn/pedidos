package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.StatusPedido;
import br.com.pedidos.api.infrastructure.messaging.ResultadoPagamentoMensagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static br.com.pedidos.api.infrastructure.messaging.PagamentosMensageria.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa o Postgres e o RabbitMQ do infra/docker-compose.yml (a API consome pedidos.resultados). */
@SpringBootTest
@AutoConfigureMockMvc
class PagamentoFluxoIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Pedidos pedidos;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void limparFilas() {
        rabbitAdmin.purgeQueue(FILA_SOLICITACOES, false);
        rabbitAdmin.purgeQueue(FILA_RESULTADOS_ERROS, false);
        rabbitAdmin.purgeQueue(FILA_SOLICITACOES_ERROS, false);
    }

    private Pedido pedidoAberto() {
        return pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
    }

    private String corpo(UUID solicitacaoId, String cenario) {
        return "{\"solicitacaoId\":\"%s\",\"cenario\":\"%s\"}".formatted(solicitacaoId, cenario);
    }

    private void solicitar(UUID pedidoId, UUID solicitacaoId, String cenario, int esperado) throws Exception {
        mockMvc.perform(post("/pedidos/{id}/pagamentos", pedidoId)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(solicitacaoId, cenario)))
                .andExpect(status().is(esperado));
    }

    private void publicarResultado(Pedido pedido, UUID solicitacaoId, BigDecimal valor, boolean aprovado) {
        rabbitTemplate.convertAndSend(EXCHANGE_EVENTOS, CHAVE_RESULTADO,
                new ResultadoPagamentoMensagem(solicitacaoId, pedido.id(), valor, aprovado, "TESTE"),
                mp -> {
                    mp.getMessageProperties().setHeader(HEADER_VERSAO, VERSAO);
                    return mp;
                });
    }

    private void aguardarStatus(UUID pedidoId, StatusPedido esperado) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (pedidos.porId(pedidoId).orElseThrow().status() == esperado) {
                return;
            }
            Thread.sleep(200);
        }
        assertEquals(esperado, pedidos.porId(pedidoId).orElseThrow().status());
    }

    @Test
    @DisplayName("POST /pagamentos: 202, mensagem v1 na fila com valor do pedido, GET mostra EM_PAGAMENTO; aprovação leva a PAGO")
    void fluxoAprovado() throws Exception {
        Pedido pedido = pedidoAberto();
        UUID solicitacaoId = UUID.randomUUID();

        solicitar(pedido.id(), solicitacaoId, "APROVAR", 202);

        Message msg = rabbitTemplate.receive(FILA_SOLICITACOES, 5000);
        assertNotNull(msg, "mensagem deveria estar em pagamentos.solicitacoes");
        assertEquals(1, ((Number) msg.getMessageProperties().getHeader(HEADER_VERSAO)).intValue());
        String json = new String(msg.getBody());
        assertTrue(json.contains(solicitacaoId.toString()) && json.contains("37.8") && json.contains("APROVAR"), json);

        mockMvc.perform(get("/pedidos/{id}", pedido.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_PAGAMENTO"));

        publicarResultado(pedido, solicitacaoId, new BigDecimal("37.80"), true);
        aguardarStatus(pedido.id(), StatusPedido.PAGO);
    }

    @Test
    @DisplayName("Recusa devolve o pedido a ABERTO")
    void fluxoRecusado() throws Exception {
        Pedido pedido = pedidoAberto();
        UUID solicitacaoId = UUID.randomUUID();
        solicitar(pedido.id(), solicitacaoId, "RECUSAR", 202);

        publicarResultado(pedido, solicitacaoId, new BigDecimal("37.80"), false);

        aguardarStatus(pedido.id(), StatusPedido.ABERTO);
    }

    @Test
    @DisplayName("Repetição com o mesmo solicitacaoId e conteúdo: 202 sem nova mensagem; outro conteúdo: 409")
    void repeticao() throws Exception {
        Pedido pedido = pedidoAberto();
        UUID solicitacaoId = UUID.randomUUID();

        solicitar(pedido.id(), solicitacaoId, "APROVAR", 202);
        solicitar(pedido.id(), solicitacaoId, "APROVAR", 202);
        solicitar(pedido.id(), solicitacaoId, "RECUSAR", 409);
        solicitar(pedido.id(), UUID.randomUUID(), "APROVAR", 409);

        assertNotNull(rabbitTemplate.receive(FILA_SOLICITACOES, 5000));
        assertNull(rabbitTemplate.receive(FILA_SOLICITACOES, 500), "não deve haver segunda mensagem");
    }

    @Test
    @DisplayName("Resultado conflitante vai para pedidos.resultados.erros e não altera o pedido")
    void resultadoConflitante() throws Exception {
        Pedido pedido = pedidoAberto();
        UUID solicitacaoId = UUID.randomUUID();
        solicitar(pedido.id(), solicitacaoId, "APROVAR", 202);
        publicarResultado(pedido, solicitacaoId, new BigDecimal("37.80"), true);
        aguardarStatus(pedido.id(), StatusPedido.PAGO);

        publicarResultado(pedido, solicitacaoId, new BigDecimal("37.80"), false);

        assertNotNull(rabbitTemplate.receive(FILA_RESULTADOS_ERROS, 10000), "deveria ir para a DLQ");
        assertEquals(StatusPedido.PAGO, pedidos.porId(pedido.id()).orElseThrow().status());
    }

    @Test
    @DisplayName("Mensagem sem header de versão vai para pedidos.resultados.erros")
    void semVersao() {
        rabbitTemplate.convertAndSend(EXCHANGE_EVENTOS, CHAVE_RESULTADO,
                new ResultadoPagamentoMensagem(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, true, "x"));

        assertNotNull(rabbitTemplate.receive(FILA_RESULTADOS_ERROS, 10000));
    }

    @Test
    @DisplayName("404 para pedido ausente; 400 para formato inválido; 409 para pedido em pagamento")
    void erros() throws Exception {
        solicitar(UUID.randomUUID(), UUID.randomUUID(), "APROVAR", 404);

        Pedido pedido = pedidoAberto();
        solicitar(pedido.id(), UUID.randomUUID(), "TALVEZ", 400);
        mockMvc.perform(post("/pedidos/{id}/pagamentos", pedido.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cenario\":\"APROVAR\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/pedidos/nao-e-uuid/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID(), "APROVAR")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/pedidos/{id}/pagamentos", pedido.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{ invalido"))
                .andExpect(status().isBadRequest());

        solicitar(pedido.id(), UUID.randomUUID(), "APROVAR", 202);
        mockMvc.perform(post("/pedidos/{id}/itens", pedido.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":\"X\",\"quantidade\":1,\"precoUnitario\":1.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /pedidos/{id} retorna 404 quando ausente")
    void getAusente() throws Exception {
        mockMvc.perform(get("/pedidos/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }
}