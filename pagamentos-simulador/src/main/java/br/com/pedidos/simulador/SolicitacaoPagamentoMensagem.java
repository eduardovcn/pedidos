package br.com.pedidos.simulador;

import java.math.BigDecimal;
import java.util.UUID;

public record SolicitacaoPagamentoMensagem(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, String cenario) {
}