package br.com.pedidos.api.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record SolicitacaoPagamento(
        UUID solicitacaoId,
        UUID pedidoId,
        BigDecimal valor,
        CenarioPagamento cenario,
        Situacao situacao) {

    public enum Situacao {
        PENDENTE_PUBLICACAO,
        PUBLICADA,
        APROVADA,
        RECUSADA
    }

    public SolicitacaoPagamento {
        Objects.requireNonNull(solicitacaoId, "solicitacaoId não pode ser nulo");
        Objects.requireNonNull(pedidoId, "pedidoId não pode ser nulo");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(cenario, "cenario não pode ser nulo");
        Objects.requireNonNull(situacao, "situacao não pode ser nula");
    }

    public static SolicitacaoPagamento nova(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, CenarioPagamento cenario) {
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario, Situacao.PENDENTE_PUBLICACAO);
    }

    public boolean concluida() {
        return situacao == Situacao.APROVADA || situacao == Situacao.RECUSADA;
    }

    public SolicitacaoPagamento publicada() {
        if (situacao != Situacao.PENDENTE_PUBLICACAO) {
            return this;
        }
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario, Situacao.PUBLICADA);
    }

    public SolicitacaoPagamento concluir(boolean aprovado) {
        return new SolicitacaoPagamento(solicitacaoId, pedidoId, valor, cenario,
                aprovado ? Situacao.APROVADA : Situacao.RECUSADA);
    }
}