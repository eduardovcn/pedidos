package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.domain.SolicitacaoPagamento;

public interface PublicadorSolicitacaoPagamento {

    /**
     * Publica a solicitação e só retorna após confirmação do broker e verificação de roteamento.
     *
     * @throws br.com.pedidos.api.application.exception.FalhaPublicacaoException se não houver confirmação positiva
     */
    void publicar(SolicitacaoPagamento solicitacao);
}