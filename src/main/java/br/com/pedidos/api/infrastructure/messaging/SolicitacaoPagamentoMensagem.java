package br.com.pedidos.api.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitacaoPagamentoMensagem(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, String cenario) {
}