package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.ItemInvalidoException;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AdicionarItemServiceTest {

    private InMemoryPedidos pedidosEmMemoria;
    private AdicionarItemService service;

    @BeforeEach
    void setUp() {
        pedidosEmMemoria = new InMemoryPedidos();
        service = new AdicionarItemService(pedidosEmMemoria);
    }

    @Test
    @DisplayName("Deve adicionar item com sucesso a pedido existente, acumulando itens e total")
    void deveAdicionarItemComSucesso() {
        var pedidoOriginal = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
        pedidosEmMemoria.salvar(pedidoOriginal);

        var pedidoAtualizado = service.executar(pedidoOriginal.id(), "CAFE-500", 1, new BigDecimal("18.90"));

        assertNotNull(pedidoAtualizado);
        assertEquals(pedidoOriginal.id(), pedidoAtualizado.id());
        assertEquals("c-1", pedidoAtualizado.clienteId());
        assertEquals(StatusPedido.ABERTO, pedidoAtualizado.status());
        assertEquals(2, pedidoAtualizado.itens().size());
        assertEquals(new BigDecimal("56.70"), pedidoAtualizado.total());

        var noBanco = pedidosEmMemoria.porId(pedidoOriginal.id());
        assertTrue(noBanco.isPresent());
        assertEquals(new BigDecimal("56.70"), noBanco.get().total());
    }

    @Test
    @DisplayName("Deve lançar PedidoNaoEncontradoException para ID inexistente")
    void deveLancarExcecaoQuandoPedidoNaoExiste() {
        UUID idInexistente = UUID.randomUUID();

        assertThrows(PedidoNaoEncontradoException.class, () ->
                service.executar(idInexistente, "CAFE-500", 1, new BigDecimal("18.90")));
    }

    @Test
    @DisplayName("Deve propagar IllegalStateException quando pedido estiver fechado (pago/cancelado)")
    void deveRecusarAdicionarItemEmPedidoFechado() {
        var pedidoPago = Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 1, new BigDecimal("18.90")))
                .pagar();
        pedidosEmMemoria.salvar(pedidoPago);

        assertThrows(IllegalStateException.class, () ->
                service.executar(pedidoPago.id(), "OUTRO", 1, new BigDecimal("10.00")));
    }

    @Test
    @DisplayName("Deve propagar ItemInvalidoException quando quantidade for inválida")
    void deveRecusarQuantidadeInvalida() {
        var pedido = Pedido.novo("c-1");
        pedidosEmMemoria.salvar(pedido);

        assertThrows(ItemInvalidoException.class, () ->
                service.executar(pedido.id(), "CAFE-500", 0, new BigDecimal("18.90")));
    }

    @Test
    @DisplayName("Deve propagar ItemInvalidoException quando preço unitário for inválido")
    void deveRecusarPrecoInvalido() {
        var pedido = Pedido.novo("c-1");
        pedidosEmMemoria.salvar(pedido);

        assertThrows(ItemInvalidoException.class, () ->
                service.executar(pedido.id(), "CAFE-500", 1, BigDecimal.ZERO));
    }

    static class InMemoryPedidos implements Pedidos {
        private final Map<UUID, Pedido> banco = new HashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            banco.put(pedido.id(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> porId(UUID id) {
            return Optional.ofNullable(banco.get(id));
        }
    }
}
