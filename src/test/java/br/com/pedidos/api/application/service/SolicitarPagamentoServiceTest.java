package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.SolicitacaoPagamentoConflitoException;
import br.com.pedidos.api.application.service.PagamentoFakes.PedidosEmMemoria;
import br.com.pedidos.api.application.service.PagamentoFakes.PublicadorFake;
import br.com.pedidos.api.application.service.PagamentoFakes.SolicitacoesEmMemoria;
import br.com.pedidos.api.application.service.PagamentoFakes.TransacaoDireta;
import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.PedidoFechadoException;
import br.com.pedidos.api.domain.SolicitacaoPagamento.Situacao;
import br.com.pedidos.api.domain.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolicitarPagamentoServiceTest {

    private PedidosEmMemoria pedidos;
    private SolicitacoesEmMemoria solicitacoes;
    private PublicadorFake publicador;
    private SolicitarPagamentoService service;
    private Pedido pedido;

    @BeforeEach
    void preparar() {
        pedidos = new PedidosEmMemoria();
        solicitacoes = new SolicitacoesEmMemoria();
        publicador = new PublicadorFake();
        service = new SolicitarPagamentoService(pedidos, solicitacoes, publicador, new TransacaoDireta());
        pedido = pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
    }

    @Test
    @DisplayName("Publica, usa o valor do pedido, move para EM_PAGAMENTO e marca PUBLICADA")
    void solicitaPagamento() {
        var s = service.executar(pedido.id(), UUID.randomUUID(), CenarioPagamento.APROVAR);

        assertEquals(Situacao.PUBLICADA, s.situacao());
        assertEquals(new BigDecimal("37.80"), s.valor());
        assertEquals(1, publicador.publicadas.size());
        assertEquals(StatusPedido.EM_PAGAMENTO, pedidos.dados.get(pedido.id()).status());
    }

    @Test
    @DisplayName("Pedido ausente lança PedidoNaoEncontradoException sem publicar")
    void pedidoAusente() {
        assertThrows(PedidoNaoEncontradoException.class,
                () -> service.executar(UUID.randomUUID(), UUID.randomUUID(), CenarioPagamento.APROVAR));
        assertTrue(publicador.publicadas.isEmpty());
    }

    @Test
    @DisplayName("Pedido fora de ABERTO (outra solicitação) lança PedidoFechadoException")
    void estadoIncompativel() {
        service.executar(pedido.id(), UUID.randomUUID(), CenarioPagamento.APROVAR);

        assertThrows(PedidoFechadoException.class,
                () -> service.executar(pedido.id(), UUID.randomUUID(), CenarioPagamento.APROVAR));
        assertEquals(1, publicador.publicadas.size());
    }

    @Test
    @DisplayName("Pedido sem itens é recusado")
    void pedidoSemItens() {
        var vazio = pedidos.salvar(Pedido.novo("c-2"));

        assertThrows(SolicitacaoPagamentoConflitoException.class,
                () -> service.executar(vazio.id(), UUID.randomUUID(), CenarioPagamento.APROVAR));
    }

    @Test
    @DisplayName("Repetição com o mesmo conteúdo retorna a mesma solicitação sem republicar")
    void repeticaoIdempotente() {
        var id = UUID.randomUUID();
        var primeira = service.executar(pedido.id(), id, CenarioPagamento.APROVAR);
        var segunda = service.executar(pedido.id(), id, CenarioPagamento.APROVAR);

        assertEquals(primeira, segunda);
        assertEquals(1, publicador.publicadas.size());
    }

    @Test
    @DisplayName("Mesmo solicitacaoId com outro cenário ou outro pedido lança conflito")
    void chaveReutilizadaComOutroConteudo() {
        var id = UUID.randomUUID();
        service.executar(pedido.id(), id, CenarioPagamento.APROVAR);
        var outro = pedidos.salvar(Pedido.novo("c-3").adicionarItem(new ItemPedido("X", 1, BigDecimal.TEN)));

        assertThrows(SolicitacaoPagamentoConflitoException.class,
                () -> service.executar(pedido.id(), id, CenarioPagamento.RECUSAR));
        assertThrows(SolicitacaoPagamentoConflitoException.class,
                () -> service.executar(outro.id(), id, CenarioPagamento.APROVAR));
    }

    @Test
    @DisplayName("Falha na publicação mantém estado pendente; reenvio com o mesmo id republica")
    void recuperaAposFalhaDePublicacao() {
        var id = UUID.randomUUID();
        publicador.falhar = true;

        assertThrows(FalhaPublicacaoException.class,
                () -> service.executar(pedido.id(), id, CenarioPagamento.APROVAR));
        assertEquals(Situacao.PENDENTE_PUBLICACAO, solicitacoes.dados.get(id).situacao());
        assertEquals(StatusPedido.EM_PAGAMENTO, pedidos.dados.get(pedido.id()).status());

        publicador.falhar = false;
        var s = service.executar(pedido.id(), id, CenarioPagamento.APROVAR);

        assertEquals(Situacao.PUBLICADA, s.situacao());
        assertEquals(1, publicador.publicadas.size());
    }
}