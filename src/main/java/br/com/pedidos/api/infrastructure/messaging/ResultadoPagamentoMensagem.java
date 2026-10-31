package br.com.pedidos.api.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoPagamentoMensagem(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, Boolean aprovado, String motivo) {
}