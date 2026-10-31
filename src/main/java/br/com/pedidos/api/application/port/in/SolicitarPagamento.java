package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento;

import java.util.UUID;

public interface SolicitarPagamento {

    SolicitacaoPagamento executar(UUID pedidoId, UUID solicitacaoId, CenarioPagamento cenario);
}