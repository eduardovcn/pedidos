package br.com.pedidos.api.adapters.entrada.rest.dto;

import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.StatusPedido;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        String clienteId,
        StatusPedido status,
        List<ItemResponse> itens,
        BigDecimal total
) {
    public static PedidoResponse de(Pedido pedido) {
        List<ItemResponse> itensResponse = pedido.itens().stream()
                .map(ItemResponse::de)
                .toList();

        return new PedidoResponse(
                pedido.id(),
                pedido.clienteId(),
                pedido.status(),
                itensResponse,
                pedido.total()
        );
    }
}
