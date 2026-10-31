package br.com.pedidos.api.infrastructure.persistence;

import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.infrastructure.persistence.entity.PedidoJpaEntity;
import br.com.pedidos.api.infrastructure.persistence.mapper.PedidoJpaMapper;
import br.com.pedidos.api.infrastructure.persistence.repository.SpringDataPedidoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PedidosJpaAdapter implements Pedidos {

    private final SpringDataPedidoRepository repository;

    public PedidosJpaAdapter(SpringDataPedidoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository não pode ser nulo");
    }

    @Override
    @Transactional
    public Pedido salvar(Pedido pedido) {
        PedidoJpaEntity entidade = PedidoJpaMapper.paraEntidade(pedido);
        PedidoJpaEntity salva = repository.save(entidade);
        return PedidoJpaMapper.paraDominio(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Pedido> porId(UUID id) {
        return repository.buscarPorIdComItens(id)
                .map(PedidoJpaMapper::paraDominio);
    }

    @Override
    @Transactional
    public Optional<Pedido> porIdParaAtualizacao(UUID id) {
        return repository.buscarPorIdComBloqueio(id)
                .map(PedidoJpaMapper::paraDominio);
    }
}