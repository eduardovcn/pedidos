package br.com.pedidos.api.infrastructure.persistence;

import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import br.com.pedidos.api.infrastructure.persistence.entity.SolicitacaoPagamentoJpaEntity;
import br.com.pedidos.api.infrastructure.persistence.repository.SpringDataSolicitacaoPagamentoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SolicitacoesPagamentoJpaAdapter implements SolicitacoesPagamento {

    private final SpringDataSolicitacaoPagamentoRepository repository;

    public SolicitacoesPagamentoJpaAdapter(SpringDataSolicitacaoPagamentoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository não pode ser nulo");
    }

    @Override
    @Transactional
    public SolicitacaoPagamento salvar(SolicitacaoPagamento s) {
        return paraDominio(repository.save(new SolicitacaoPagamentoJpaEntity(
                s.solicitacaoId(), s.pedidoId(), s.valor(), s.cenario(), s.situacao())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitacaoPagamento> porId(UUID solicitacaoId) {
        return repository.findById(solicitacaoId).map(SolicitacoesPagamentoJpaAdapter::paraDominio);
    }

    private static SolicitacaoPagamento paraDominio(SolicitacaoPagamentoJpaEntity e) {
        return new SolicitacaoPagamento(e.getSolicitacaoId(), e.getPedidoId(), e.getValor(), e.getCenario(), e.getSituacao());
    }
}