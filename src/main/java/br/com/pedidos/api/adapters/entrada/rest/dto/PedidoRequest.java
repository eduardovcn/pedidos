package br.com.pedidos.api.adapters.entrada.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PedidoRequest(
        @NotBlank(message = "clienteId é obrigatório")
        String clienteId,

        @NotNull(message = "itens é obrigatório")
        @Valid
        List<ItemRequest> itens
) {}
