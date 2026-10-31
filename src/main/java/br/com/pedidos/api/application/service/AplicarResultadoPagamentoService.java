package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.model.ResultadoPagamento;
import br.com.pedidos.api.application.port.in.AplicarResultadoPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.application.port.out.Transacao;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import br.com.pedidos.api.domain.StatusPedido;

import java.util.Objects;

public class AplicarResultadoPagamentoService implements AplicarResultadoPagamento {

    private final Pedidos pedidos;
    private final SolicitacoesPagamento solicitacoes;
    private final Transacao transacao;

    public AplicarResultadoPagamentoService(Pedidos pedidos, SolicitacoesPagamento solicitacoes, Transacao transacao) {
        this.pedidos = Objects.requireNonNull(pedidos, "pedidos não pode ser nulo");
        this.solicitacoes = Objects.requireNonNull(solicitacoes, "solicitacoes não pode ser nulo");
        this.transacao = Objects.requireNonNull(transacao, "transacao não pode ser nulo");
    }

    @Override
    public void executar(ResultadoPagamento resultado) {
        Objects.requireNonNull(resultado, "resultado não pode ser nulo");
        transacao.executar(() -> {
            aplicar(resultado);
            return null;
        });
    }

    private void aplicar(ResultadoPagamento resultado) {
        SolicitacaoPagamento solicitacao = solicitacoes.porId(resultado.solicitacaoId())
                .orElseThrow(() -> new ResultadoPagamentoInvalidoException(
                        "Solicitação desconhecida: " + resultado.solicitacaoId()));

        if (!solicitacao.pedidoId().equals(resultado.pedidoId())
                || solicitacao.valor().compareTo(resultado.valor()) != 0) {
            throw new ResultadoPagamentoInvalidoException(
                    "Resultado diverge da solicitação " + resultado.solicitacaoId() + " (pedidoId ou valor)");
        }

        if (solicitacao.concluida()) {
            boolean aprovadoAntes = solicitacao.situacao() == SolicitacaoPagamento.Situacao.APROVADA;
            if (aprovadoAntes == resultado.aprovado()) {
                return; // duplicata idempotente
            }
            throw new ResultadoPagamentoInvalidoException(
                    "Resultado conflitante para a solicitação " + resultado.solicitacaoId());
        }

        Pedido pedido = pedidos.porIdParaAtualizacao(solicitacao.pedidoId())
                .orElseThrow(() -> new ResultadoPagamentoInvalidoException(
                        "Pedido inexistente: " + solicitacao.pedidoId()));
        if (pedido.status() != StatusPedido.EM_PAGAMENTO) {
            throw new ResultadoPagamentoInvalidoException(
                    "Pedido " + pedido.id() + " não está EM_PAGAMENTO (status " + pedido.status() + ")");
        }

        pedidos.salvar(resultado.aprovado() ? pedido.confirmarPagamento() : pedido.recusarPagamento());
        solicitacoes.salvar(solicitacao.concluir(resultado.aprovado()));
    }
}