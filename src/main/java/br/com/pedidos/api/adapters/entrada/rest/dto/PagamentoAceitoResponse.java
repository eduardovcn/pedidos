package br.com.pedidos.api.adapters.entrada.rest.dto;

import br.com.pedidos.api.domain.SolicitacaoPagamento;

import java.util.UUID;

/** 202 significa "solicitação entregue ao broker", nunca "pagamento aprovado". */
public record PagamentoAceitoResponse(UUID solicitacaoId, UUID pedidoId, SolicitacaoPagamento.Situacao situacao) {

    public static PagamentoAceitoResponse de(SolicitacaoPagamento s) {
        return new PagamentoAceitoResponse(s.solicitacaoId(), s.pedidoId(), s.situacao());
    }
}