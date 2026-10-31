package br.com.pedidos.api.adapters.entrada.rest.dto;

import br.com.pedidos.api.domain.CenarioPagamento;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** O valor nunca vem do cliente: é calculado a partir do pedido. */
public record PagamentoRequest(
        @NotNull UUID solicitacaoId,
        @NotNull CenarioPagamento cenario
) {
}