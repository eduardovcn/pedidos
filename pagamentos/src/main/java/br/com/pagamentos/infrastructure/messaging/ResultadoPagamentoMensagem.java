package br.com.pagamentos.infrastructure.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoPagamentoMensagem(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, boolean aprovado,
                                         String motivo) {
}
