package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.exception.SolicitacaoPagamentoConflitoException;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.application.port.out.Transacao;
import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.SolicitacaoPagamento;

import java.util.Objects;
import java.util.UUID;

/**
 * Não existe transação única entre Postgres e RabbitMQ: primeiro grava o estado (transação local),
 * depois publica. Se a publicação falhar, o estado persistido permite reenviar com o mesmo solicitacaoId.
 */
public class SolicitarPagamentoService implements SolicitarPagamento {

    private final Pedidos pedidos;
    private final SolicitacoesPagamento solicitacoes;
    private final PublicadorSolicitacaoPagamento publicador;
    private final Transacao transacao;

    public SolicitarPagamentoService(Pedidos pedidos, SolicitacoesPagamento solicitacoes,
                                     PublicadorSolicitacaoPagamento publicador, Transacao transacao) {
        this.pedidos = Objects.requireNonNull(pedidos, "pedidos não pode ser nulo");
        this.solicitacoes = Objects.requireNonNull(solicitacoes, "solicitacoes não pode ser nulo");
        this.publicador = Objects.requireNonNull(publicador, "publicador não pode ser nulo");
        this.transacao = Objects.requireNonNull(transacao, "transacao não pode ser nulo");
    }

    @Override
    public SolicitacaoPagamento executar(UUID pedidoId, UUID solicitacaoId, CenarioPagamento cenario) {
        Objects.requireNonNull(pedidoId, "pedidoId não pode ser nulo");
        Objects.requireNonNull(solicitacaoId, "solicitacaoId não pode ser nulo");
        Objects.requireNonNull(cenario, "cenario não pode ser nulo");

        SolicitacaoPagamento registrada = transacao.executar(() -> registrar(pedidoId, solicitacaoId, cenario));

        if (registrada.situacao() != SolicitacaoPagamento.Situacao.PENDENTE_PUBLICACAO) {
            return registrada;
        }

        publicador.publicar(registrada);

        return transacao.executar(() -> {
            SolicitacaoPagamento atual = solicitacoes.porId(solicitacaoId).orElse(registrada);
            SolicitacaoPagamento publicada = atual.publicada();
            return publicada.equals(atual) ? atual : solicitacoes.salvar(publicada);
        });
    }

    private SolicitacaoPagamento registrar(UUID pedidoId, UUID solicitacaoId, CenarioPagamento cenario) {
        Pedido pedido = pedidos.porIdParaAtualizacao(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado para o ID: " + pedidoId));

        var existente = solicitacoes.porId(solicitacaoId);
        if (existente.isPresent()) {
            SolicitacaoPagamento s = existente.get();
            if (!s.pedidoId().equals(pedidoId) || s.cenario() != cenario) {
                throw new SolicitacaoPagamentoConflitoException(
                        "solicitacaoId " + solicitacaoId + " já foi usado com outro conteúdo");
            }
            return s;
        }

        if (pedido.itens().isEmpty()) {
            throw new SolicitacaoPagamentoConflitoException("Pedido sem itens não pode ser pago");
        }

        pedidos.salvar(pedido.iniciarPagamento());
        return solicitacoes.salvar(SolicitacaoPagamento.nova(solicitacaoId, pedidoId, pedido.total(), cenario));
    }
}