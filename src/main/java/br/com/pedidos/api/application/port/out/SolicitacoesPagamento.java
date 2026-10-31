package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.domain.SolicitacaoPagamento;

import java.util.Optional;
import java.util.UUID;

public interface SolicitacoesPagamento {

    SolicitacaoPagamento salvar(SolicitacaoPagamento solicitacao);

    Optional<SolicitacaoPagamento> porId(UUID solicitacaoId);
}