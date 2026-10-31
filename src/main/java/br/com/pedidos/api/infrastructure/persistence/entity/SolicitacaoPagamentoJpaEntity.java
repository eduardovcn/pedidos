package br.com.pedidos.api.infrastructure.persistence.entity;

import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "solicitacao_pagamento")
public class SolicitacaoPagamentoJpaEntity {

    @Id
    private UUID solicitacaoId;

    @Column(nullable = false)
    private UUID pedidoId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CenarioPagamento cenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SolicitacaoPagamento.Situacao situacao;

    protected SolicitacaoPagamentoJpaEntity() {
    }

    public SolicitacaoPagamentoJpaEntity(UUID solicitacaoId, UUID pedidoId, BigDecimal valor,
                                         CenarioPagamento cenario, SolicitacaoPagamento.Situacao situacao) {
        this.solicitacaoId = solicitacaoId;
        this.pedidoId = pedidoId;
        this.valor = valor;
        this.cenario = cenario;
        this.situacao = situacao;
    }

    public UUID getSolicitacaoId() {
        return solicitacaoId;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public CenarioPagamento getCenario() {
        return cenario;
    }

    public SolicitacaoPagamento.Situacao getSituacao() {
        return situacao;
    }
}