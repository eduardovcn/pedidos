package br.com.pedidos.api.adapters.entrada.rest.dto;

import br.com.pedidos.api.domain.ItemPedido;

import java.math.BigDecimal;

public record ItemResponse(
        String produtoId,
        int quantidade,
        BigDecimal precoUnitario
) {
    public static ItemResponse de(ItemPedido item) {
        return new ItemResponse(item.produtoId(), item.quantidade(), item.precoUnitario());
    }
}
