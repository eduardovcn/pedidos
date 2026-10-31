package br.com.pedidos.api.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoPagamento(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, boolean aprovado, String motivo) {
}