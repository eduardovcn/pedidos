package br.com.pedidos.api.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Pedido(UUID id, String clienteId, StatusPedido status, List<ItemPedido> itens) {

    public Pedido {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(clienteId, "clienteId não pode ser nulo");
        if (clienteId.isBlank()) {
            throw new IllegalArgumentException("clienteId não pode estar em branco");
        }
        Objects.requireNonNull(status, "status não pode ser nulo");
        itens = (itens == null) ? List.of() : List.copyOf(itens);
    }

    public static Pedido novo(String clienteId) {
        return new Pedido(UUID.randomUUID(), clienteId, StatusPedido.ABERTO, List.of());
    }

    public BigDecimal total() {
        return itens.stream()
                .map(ItemPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Pedido adicionarItem(ItemPedido item) {
        Objects.requireNonNull(item, "Item não pode ser nulo");
        if (status != StatusPedido.ABERTO) {
            throw new PedidoFechadoException("Não é possível adicionar item a um pedido com status " + status);
        }
        List<ItemPedido> novosItens = new ArrayList<>(itens);
        novosItens.add(item);
        return new Pedido(id, clienteId, status, novosItens);
    }

    public Pedido iniciarPagamento() {
        if (status != StatusPedido.ABERTO) {
            throw new PedidoFechadoException("Apenas pedidos ABERTO podem iniciar pagamento. Status atual: " + status);
        }
        return new Pedido(id, clienteId, StatusPedido.EM_PAGAMENTO, itens);
    }

    public Pedido confirmarPagamento() {
        if (status != StatusPedido.EM_PAGAMENTO) {
            throw new PedidoFechadoException("Apenas pedidos EM_PAGAMENTO podem ser pagos. Status atual: " + status);
        }
        return new Pedido(id, clienteId, StatusPedido.PAGO, itens);
    }

    public Pedido recusarPagamento() {
        if (status != StatusPedido.EM_PAGAMENTO) {
            throw new PedidoFechadoException("Apenas pedidos EM_PAGAMENTO podem ter o pagamento recusado. Status atual: " + status);
        }
        return new Pedido(id, clienteId, StatusPedido.ABERTO, itens);
    }
    public Pedido cancelar() {
        if (status != StatusPedido.ABERTO) {
            throw new PedidoFechadoException("Apenas pedidos ABERTO podem ser cancelados. Status atual: " + status);
        }
        return new Pedido(id, clienteId, StatusPedido.CANCELADO, itens);
    }
}
