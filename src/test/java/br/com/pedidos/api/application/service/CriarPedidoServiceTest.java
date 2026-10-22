package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.port.in.CriarPedido.ItemComando;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.StatusPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CriarPedidoServiceTest {

    private InMemoryPedidos pedidosEmMemoria;
    private CriarPedidoService service;

    @BeforeEach
    void setUp() {
        pedidosEmMemoria = new InMemoryPedidos();
        service = new CriarPedidoService(pedidosEmMemoria);
    }

    @Test
    @DisplayName("Deve criar e salvar pedido com itens válidos e devolver pedido salvo com total correto")
    void deveCriarESalvarPedidoComSucesso() {
        var itens = List.of(
                new ItemComando("CAFE-500", 2, new BigDecimal("18.90")),
                new ItemComando("LEITE-1L", 1, new BigDecimal("5.50"))
        );

        var pedidoSalvo = service.executar("c-1", itens);

        assertNotNull(pedidoSalvo.id());
        assertEquals("c-1", pedidoSalvo.clienteId());
        assertEquals(StatusPedido.ABERTO, pedidoSalvo.status());
        assertEquals(2, pedidoSalvo.itens().size());
        assertEquals(new BigDecimal("43.30"), pedidoSalvo.total());

        var pedidoNoRepositorio = pedidosEmMemoria.porId(pedidoSalvo.id());
        assertTrue(pedidoNoRepositorio.isPresent());
        assertEquals(pedidoSalvo, pedidoNoRepositorio.get());
    }

    @Test
    @DisplayName("Deve recusar criação quando lista de itens for vazia")
    void deveRecusarListaDeItensVazia() {
        assertThrows(IllegalArgumentException.class, () ->
                service.executar("c-1", List.of()));
    }

    @Test
    @DisplayName("Deve recusar criação quando lista de itens for nula")
    void deveRecusarListaDeItensNula() {
        assertThrows(IllegalArgumentException.class, () ->
                service.executar("c-1", null));
    }

    @Test
    @DisplayName("Deve recusar criação quando cliente for nulo ou em branco")
    void deveRecusarClienteInvalido() {
        var itens = List.of(new ItemComando("CAFE-500", 1, new BigDecimal("18.90")));

        assertThrows(NullPointerException.class, () ->
                service.executar(null, itens));

        assertThrows(IllegalArgumentException.class, () ->
                service.executar("   ", itens));
    }

    @Test
    @DisplayName("Deve propagar validações do domínio caso item tenha valores inválidos")
    void devePropagarValidacoesDoDominioParaItensInvalidos() {
        var itemQuantidadeInvalida = List.of(new ItemComando("CAFE-500", 0, new BigDecimal("18.90")));
        assertThrows(IllegalArgumentException.class, () ->
                service.executar("c-1", itemQuantidadeInvalida));

        var itemPrecoInvalido = List.of(new ItemComando("CAFE-500", 1, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                service.executar("c-1", itemPrecoInvalido));
    }

    static class InMemoryPedidos implements Pedidos {
        private final Map<UUID, Pedido> bancoEmMemoria = new HashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            bancoEmMemoria.put(pedido.id(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> porId(UUID id) {
            return Optional.ofNullable(bancoEmMemoria.get(id));
        }
    }
}
