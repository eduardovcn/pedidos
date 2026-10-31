package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.application.port.out.Transacao;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.SolicitacaoPagamento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

final class PagamentoFakes {

    private PagamentoFakes() {
    }

    static class PedidosEmMemoria implements Pedidos {
        final Map<UUID, Pedido> dados = new HashMap<>();

        @Override
        public Pedido salvar(Pedido pedido) {
            dados.put(pedido.id(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> porId(UUID id) {
            return Optional.ofNullable(dados.get(id));
        }
    }

    static class SolicitacoesEmMemoria implements SolicitacoesPagamento {
        final Map<UUID, SolicitacaoPagamento> dados = new HashMap<>();

        @Override
        public SolicitacaoPagamento salvar(SolicitacaoPagamento s) {
            dados.put(s.solicitacaoId(), s);
            return s;
        }

        @Override
        public Optional<SolicitacaoPagamento> porId(UUID id) {
            return Optional.ofNullable(dados.get(id));
        }
    }

    static class PublicadorFake implements PublicadorSolicitacaoPagamento {
        final List<SolicitacaoPagamento> publicadas = new ArrayList<>();
        boolean falhar;

        @Override
        public void publicar(SolicitacaoPagamento solicitacao) {
            if (falhar) {
                throw new FalhaPublicacaoException("broker indisponível");
            }
            publicadas.add(solicitacao);
        }
    }

    static class TransacaoDireta implements Transacao {
        @Override
        public <T> T executar(Supplier<T> trabalho) {
            return trabalho.get();
        }
    }
}