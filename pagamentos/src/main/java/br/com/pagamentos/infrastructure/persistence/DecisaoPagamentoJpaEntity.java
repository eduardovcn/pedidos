package br.com.pagamentos.infrastructure.persistence;

import br.com.pagamentos.domain.CenarioPagamento;
import br.com.pagamentos.domain.DecisaoPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A chave primária garante uma única decisão por solicitacaoId. */
@Entity
@Table(name = "decisao_pagamento")
public class DecisaoPagamentoJpaEntity implements Persistable<UUID> {

    @Id
    @Column(name = "solicitacao_id", nullable = false, updatable = false)
    private UUID solicitacaoId;

    @Column(name = "pedido_id", nullable = false, updatable = false)
    private UUID pedidoId;

    @Column(name = "valor", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "cenario", nullable = false, updatable = false, length = 20)
    private CenarioPagamento cenario;

    @Column(name = "aprovado", nullable = false, updatable = false)
    private boolean aprovado;

    @Column(name = "motivo", nullable = false, updatable = false, length = 40)
    private String motivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Transient
    private boolean novo = false;

    protected DecisaoPagamentoJpaEntity() {
    }

    public static DecisaoPagamentoJpaEntity de(DecisaoPagamento d) {
        var e = new DecisaoPagamentoJpaEntity();
        e.solicitacaoId = d.solicitacaoId();
        e.pedidoId = d.pedidoId();
        e.valor = d.valor();
        e.cenario = d.cenario();
        e.aprovado = d.aprovado();
        e.motivo = d.motivo();
        e.criadoEm = Instant.now();
        e.novo = true;
        return e;
    }

    public DecisaoPagamento paraDominio() {
        return new DecisaoPagamento(solicitacaoId, pedidoId, valor, cenario, aprovado, motivo);
    }

    @Override
    public UUID getId() {
        return solicitacaoId;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        this.novo = false;
    }
}
