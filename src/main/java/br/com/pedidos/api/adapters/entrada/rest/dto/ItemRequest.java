package br.com.pedidos.api.adapters.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank(message = "produtoId é obrigatório")
        String produtoId,

        @NotNull(message = "quantidade é obrigatória")
        Integer quantidade,

        @NotNull(message = "precoUnitario é obrigatório")
        BigDecimal precoUnitario
) {}
