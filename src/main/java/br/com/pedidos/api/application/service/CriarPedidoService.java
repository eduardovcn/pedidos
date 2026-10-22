package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoSemItensException;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;

import java.util.List;
import java.util.Objects;

public class CriarPedidoService implements CriarPedido {

    private final Pedidos pedidos;

    public CriarPedidoService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos, "pedidos não pode ser nulo");
    }

    @Override
    public Pedido executar(String clienteId, List<ItemComando> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoSemItensException("Lista de itens não pode ser nula ou vazia");
        }

        Pedido pedido = Pedido.novo(clienteId);
        for (ItemComando item : itens) {
            Objects.requireNonNull(item, "item não pode ser nulo");
            pedido = pedido.adicionarItem(new ItemPedido(item.produtoId(), item.quantidade(), item.precoUnitario()));
        }

        return pedidos.salvar(pedido);
    }
}
