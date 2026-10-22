package br.com.pedidos.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PedidoTest {

    @Test
    @DisplayName("Caso 1: Pedido.novo deve criar rascunho com UUID, clienteId, status ABERTO, sem itens e total ZERO")
    void deveCriarNovoPedidoComoRascunhoAberto() {
        var pedido = Pedido.novo("c-1");

        assertNotNull(pedido.id());
        assertEquals("c-1", pedido.clienteId());
        assertEquals(StatusPedido.ABERTO, pedido.status());
        assertTrue(pedido.itens().isEmpty());
        assertEquals(BigDecimal.ZERO, pedido.total());
    }

    @Test
    @DisplayName("Caso 2: adicionarItem deve devolver novo objeto mantendo a instância anterior inalterada")
    void deveAdicionarItemRetornandoNovoObjetoSemModificarAnterior() {
        var pedidoInicial = Pedido.novo("c-1");
        var item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        var pedidoComItem = pedidoInicial.adicionarItem(item);

        assertNotSame(pedidoInicial, pedidoComItem);
        assertTrue(pedidoInicial.itens().isEmpty());
        assertEquals(BigDecimal.ZERO, pedidoInicial.total());

        assertEquals(1, pedidoComItem.itens().size());
        assertEquals(item, pedidoComItem.itens().get(0));
        assertEquals(new BigDecimal("37.80"), pedidoComItem.total());
        assertEquals(pedidoInicial.id(), pedidoComItem.id());
        assertEquals(pedidoInicial.clienteId(), pedidoComItem.clienteId());
        assertEquals(StatusPedido.ABERTO, pedidoComItem.status());
    }

    @Test
    @DisplayName("Casos 3 e 4: Deve acumular múltiplos itens e calcular total derivado corretamente")
    void deveAcumularMultiplosItensECalcularTotalDerivado() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")))
                .adicionarItem(new ItemPedido("LEITE-1L", 3, new BigDecimal("5.50")));

        assertEquals(2, pedido.itens().size());
        assertEquals(new BigDecimal("54.30"), pedido.total());
    }

    @Test
    @DisplayName("Caso 5: Transição de ABERTO para PAGO deve retornar nova instância com status PAGO")
    void devePagarPedidoAbertoRetornandoNovaInstancia() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

        var pedidoPago = pedido.pagar();

        assertNotSame(pedido, pedidoPago);
        assertEquals(StatusPedido.ABERTO, pedido.status());
        assertEquals(StatusPedido.PAGO, pedidoPago.status());
        assertEquals(pedido.id(), pedidoPago.id());
        assertEquals(pedido.itens(), pedidoPago.itens());
        assertEquals(new BigDecimal("37.80"), pedidoPago.total());
    }

    @Test
    @DisplayName("Caso 6: Transição de ABERTO para CANCELADO deve retornar nova instância com status CANCELADO")
    void deveCancelarPedidoAbertoRetornandoNovaInstancia() {
        var pedido = Pedido.novo("c-1");

        var pedidoCancelado = pedido.cancelar();

        assertNotSame(pedido, pedidoCancelado);
        assertEquals(StatusPedido.ABERTO, pedido.status());
        assertEquals(StatusPedido.CANCELADO, pedidoCancelado.status());
        assertEquals(pedido.id(), pedidoCancelado.id());
    }

    @Test
    @DisplayName("Caso 7: Lista de itens deve ser imutável externamente")
    void deveImpedirModificacaoExternaDaListaDeItens() {
        var pedido = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));

        var itens = pedido.itens();
        var novoItem = new ItemPedido("NOVO-1", 1, BigDecimal.TEN);

        assertThrows(UnsupportedOperationException.class, () -> itens.add(novoItem));
        assertThrows(UnsupportedOperationException.class, itens::clear);
    }

    @Test
    @DisplayName("Caso 10: Deve recusar adição de item em pedido PAGO")
    void deveRecusarAdicionarItemEmPedidoPago() {
        var pedidoPago = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")))
                .pagar();

        var novoItem = new ItemPedido("NOVO-1", 1, BigDecimal.TEN);

        assertThrows(IllegalStateException.class, () -> pedidoPago.adicionarItem(novoItem));
    }

    @Test
    @DisplayName("Caso 11: Deve recusar adição de item em pedido CANCELADO")
    void deveRecusarAdicionarItemEmPedidoCancelado() {
        var pedidoCancelado = Pedido.novo("c-1").cancelar();

        var novoItem = new ItemPedido("NOVO-1", 1, BigDecimal.TEN);

        assertThrows(IllegalStateException.class, () -> pedidoCancelado.adicionarItem(novoItem));
    }

    @Test
    @DisplayName("Caso 12: Deve recusar transições a partir de pedido PAGO")
    void deveRecusarTransicoesEmPedidoPago() {
        var pedidoPago = Pedido.novo("c-1").pagar();

        assertThrows(IllegalStateException.class, pedidoPago::pagar);
        assertThrows(IllegalStateException.class, pedidoPago::cancelar);
    }

    @Test
    @DisplayName("Caso 12: Deve recusar transições a partir de pedido CANCELADO")
    void deveRecusarTransicoesEmPedidoCancelado() {
        var pedidoCancelado = Pedido.novo("c-1").cancelar();

        assertThrows(IllegalStateException.class, pedidoCancelado::pagar);
        assertThrows(IllegalStateException.class, pedidoCancelado::cancelar);
    }

    @Test
    @DisplayName("Validação: clienteId não pode ser nulo ou em branco")
    void deveRecusarClienteIdInvalido() {
        assertThrows(NullPointerException.class, () -> Pedido.novo(null));
        assertThrows(IllegalArgumentException.class, () -> Pedido.novo("   "));
    }
}
