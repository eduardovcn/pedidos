package br.com.pedidos.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ItemPedidoTest {

    @Test
    @DisplayName("Deve calcular subtotal do item corretamente (exemplo: CAFE-500: 2 x 18.90 = 37.80)")
    void deveCalcularSubtotalCorretamente() {
        var item = new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"));

        assertEquals(new BigDecimal("37.80"), item.subtotal());
    }

    @Test
    @DisplayName("Deve recusar criação de item com quantidade zero ou negativa")
    void deveRecusarQuantidadeInvalida() {
        assertThrows(IllegalArgumentException.class, () ->
                new ItemPedido("CAFE-500", 0, new BigDecimal("18.90")));

        assertThrows(IllegalArgumentException.class, () ->
                new ItemPedido("CAFE-500", -1, new BigDecimal("18.90")));
    }

    @Test
    @DisplayName("Deve recusar criação de item com preço unitário nulo, zero ou negativo")
    void deveRecusarPrecoUnitarioInvalido() {
        assertThrows(NullPointerException.class, () ->
                new ItemPedido("CAFE-500", 1, null));

        assertThrows(IllegalArgumentException.class, () ->
                new ItemPedido("CAFE-500", 1, BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                new ItemPedido("CAFE-500", 1, new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("Deve recusar criação de item com produtoId nulo ou em branco")
    void deveRecusarProdutoIdInvalido() {
        assertThrows(NullPointerException.class, () ->
                new ItemPedido(null, 1, new BigDecimal("10.00")));

        assertThrows(IllegalArgumentException.class, () ->
                new ItemPedido("   ", 1, new BigDecimal("10.00")));
    }
}
