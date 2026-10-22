package br.com.pedidos.api.infrastructure.persistence.mapper;

import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.infrastructure.persistence.entity.ItemJpaEntity;
import br.com.pedidos.api.infrastructure.persistence.entity.PedidoJpaEntity;

import java.util.ArrayList;
import java.util.List;

public final class PedidoJpaMapper {

    private PedidoJpaMapper() {
    }

    public static PedidoJpaEntity paraEntidade(Pedido dominio) {
        if (dominio == null) {
            return null;
        }

        PedidoJpaEntity entidade = new PedidoJpaEntity(
                dominio.id(),
                dominio.clienteId(),
                dominio.status()
        );

        if (dominio.itens() != null) {
            for (ItemPedido itemDominio : dominio.itens()) {
                ItemJpaEntity itemEntidade = new ItemJpaEntity(
                        itemDominio.produtoId(),
                        itemDominio.quantidade(),
                        itemDominio.precoUnitario()
                );
                entidade.adicionarItem(itemEntidade);
            }
        }

        return entidade;
    }

    public static Pedido paraDominio(PedidoJpaEntity entidade) {
        if (entidade == null) {
            return null;
        }

        List<ItemPedido> itensDominio = new ArrayList<>();
        if (entidade.getItens() != null) {
            for (ItemJpaEntity itemEntidade : entidade.getItens()) {
                itensDominio.add(new ItemPedido(
                        itemEntidade.getSku(),
                        itemEntidade.getQuantidade(),
                        itemEntidade.getPrecoUnitario()
                ));
            }
        }

        return new Pedido(
                entidade.getId(),
                entidade.getClienteId(),
                entidade.getStatus(),
                itensDominio
        );
    }
}
