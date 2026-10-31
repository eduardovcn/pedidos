package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.model.ResultadoPagamento;
import br.com.pedidos.api.application.service.PagamentoFakes.PedidosEmMemoria;
import br.com.pedidos.api.application.service.PagamentoFakes.SolicitacoesEmMemoria;
import br.com.pedidos.api.application.service.PagamentoFakes.TransacaoDireta;
import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento.Situacao;
import br.com.pedidos.api.domain.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AplicarResultadoPagamentoServiceTest {

    private PedidosEmMemoria pedidos;
    private SolicitacoesEmMemoria solicitacoes;
    private AplicarResultadoPagamentoService service;
    private Pedido pedido;
    private UUID solicitacaoId;

    @BeforeEach
    void preparar() {
        pedidos = new PedidosEmMemoria();
        solicitacoes = new SolicitacoesEmMemoria();
        service = new AplicarResultadoPagamentoService(pedidos, solicitacoes, new TransacaoDireta());
        pedido = pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")))
                .iniciarPagamento());
        solicitacaoId = UUID.randomUUID();
        solicitacoes.salvar(SolicitacaoPagamento
                .nova(solicitacaoId, pedido.id(), pedido.total(), CenarioPagamento.APROVAR).publicada());
    }

    private ResultadoPagamento resultado(boolean aprovado) {
        return new ResultadoPagamento(solicitacaoId, pedido.id(), new BigDecimal("37.8"), aprovado, "SIMULADO");
    }

    @Test
    @DisplayName("Aprovação leva o pedido a PAGO e a solicitação a APROVADA")
    void aprova() {
        service.executar(resultado(true));

        assertEquals(StatusPedido.PAGO, pedidos.dados.get(pedido.id()).status());
        assertEquals(Situacao.APROVADA, solicitacoes.dados.get(solicitacaoId).situacao());
    }

    @Test
    @DisplayName("Recusa devolve o pedido a ABERTO")
    void recusa() {
        service.executar(resultado(false));

        assertEquals(StatusPedido.ABERTO, pedidos.dados.get(pedido.id()).status());
        assertEquals(Situacao.RECUSADA, solicitacoes.dados.get(solicitacaoId).situacao());
    }

    @Test
    @DisplayName("Resultado duplicado é ignorado sem erro")
    void duplicata() {
        service.executar(resultado(true));
        assertDoesNotThrow(() -> service.executar(resultado(true)));
        assertEquals(StatusPedido.PAGO, pedidos.dados.get(pedido.id()).status());
    }

    @Test
    @DisplayName("Resultado conflitante é rejeitado e não altera o pedido")
    void conflitante() {
        service.executar(resultado(true));

        assertThrows(ResultadoPagamentoInvalidoException.class, () -> service.executar(resultado(false)));
        assertEquals(StatusPedido.PAGO, pedidos.dados.get(pedido.id()).status());
    }

    @Test
    @DisplayName("Solicitação desconhecida, pedido divergente ou valor divergente são rejeitados")
    void divergencias() {
        assertThrows(ResultadoPagamentoInvalidoException.class, () -> service.executar(
                new ResultadoPagamento(UUID.randomUUID(), pedido.id(), pedido.total(), true, "x")));
        assertThrows(ResultadoPagamentoInvalidoException.class, () -> service.executar(
                new ResultadoPagamento(solicitacaoId, UUID.randomUUID(), pedido.total(), true, "x")));
        assertThrows(ResultadoPagamentoInvalidoException.class, () -> service.executar(
                new ResultadoPagamento(solicitacaoId, pedido.id(), new BigDecimal("1.00"), true, "x")));
        assertEquals(StatusPedido.EM_PAGAMENTO, pedidos.dados.get(pedido.id()).status());
    }
}