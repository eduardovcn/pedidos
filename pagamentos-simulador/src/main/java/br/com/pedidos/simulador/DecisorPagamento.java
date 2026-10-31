package br.com.pedidos.simulador;

import java.util.Objects;

/**
 * Decisão determinística e sem estado: o mesmo pedido de pagamento sempre produz o mesmo resultado.
 * Por isso a reentrega ou duplicata de uma solicitação nunca inverte o desfecho.
 */
public final class DecisorPagamento {

    public static final String MOTIVO_APROVADO = "APROVADO_SIMULADO";
    public static final String MOTIVO_RECUSADO = "RECUSADO_SIMULADO";

    private DecisorPagamento() {
    }

    public static ResultadoPagamentoMensagem decidir(SolicitacaoPagamentoMensagem s) {
        Objects.requireNonNull(s, "solicitação não pode ser nula");
        if (s.solicitacaoId() == null || s.pedidoId() == null || s.valor() == null || s.cenario() == null) {
            throw new IllegalArgumentException("Solicitação incompleta");
        }
        boolean aprovado = switch (s.cenario()) {
            case "APROVAR" -> true;
            case "RECUSAR" -> false;
            default -> throw new IllegalArgumentException("Cenário desconhecido: " + s.cenario());
        };
        return new ResultadoPagamentoMensagem(s.solicitacaoId(), s.pedidoId(), s.valor(), aprovado,
                aprovado ? MOTIVO_APROVADO : MOTIVO_RECUSADO);
    }
}