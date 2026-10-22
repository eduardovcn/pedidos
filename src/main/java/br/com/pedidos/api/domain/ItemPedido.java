package br.com.pedidos.api.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record ItemPedido(String produtoId, int quantidade, BigDecimal precoUnitario) {

    public ItemPedido {
        Objects.requireNonNull(produtoId, "produtoId não pode ser nulo");
        if (produtoId.isBlank()) {
            throw new IllegalArgumentException("produtoId não pode estar em branco");
        }
        if (quantidade <= 0) {
            throw new ItemInvalidoException("Quantidade deve ser estritamente positiva");
        }
        Objects.requireNonNull(precoUnitario, "precoUnitario não pode ser nulo");
        if (precoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemInvalidoException("Preço unitário deve ser estritamente positivo");
        }
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
