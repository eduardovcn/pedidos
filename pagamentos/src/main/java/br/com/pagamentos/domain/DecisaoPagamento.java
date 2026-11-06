package br.com.pagamentos.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Decisão simulada de um pagamento. É determinística: o cenário pedido define o desfecho, sem aleatoriedade
 * e sem qualquer cobrança real.
 */
public record DecisaoPagamento(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, CenarioPagamento cenario,
                               boolean aprovado, String motivo) {

    public static final String MOTIVO_APROVADO = "APROVADO_SIMULADO";
    public static final String MOTIVO_RECUSADO = "RECUSADO_SIMULADO";

    public DecisaoPagamento {
        Objects.requireNonNull(solicitacaoId, "solicitacaoId não pode ser nulo");
        Objects.requireNonNull(pedidoId, "pedidoId não pode ser nulo");
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        Objects.requireNonNull(cenario, "cenario não pode ser nulo");
        Objects.requireNonNull(motivo, "motivo não pode ser nulo");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor deve ser positivo");
        }
    }

    public static DecisaoPagamento decidir(UUID solicitacaoId, UUID pedidoId, BigDecimal valor,
                                           CenarioPagamento cenario) {
        Objects.requireNonNull(cenario, "cenario não pode ser nulo");
        boolean aprovado = cenario == CenarioPagamento.APROVAR;
        return new DecisaoPagamento(solicitacaoId, pedidoId, valor, cenario, aprovado,
                aprovado ? MOTIVO_APROVADO : MOTIVO_RECUSADO);
    }

    /** Confere se uma solicitação recebida descreve a mesma decisão (valor comparado por compareTo). */
    public boolean corresponde(UUID pedidoId, BigDecimal valor, CenarioPagamento cenario) {
        return this.pedidoId.equals(pedidoId)
                && this.valor.compareTo(valor) == 0
                && this.cenario == cenario;
    }
}
