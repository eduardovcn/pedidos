package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class AdicionarItemService implements AdicionarItem {

    private final Pedidos pedidos;

    public AdicionarItemService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos, "pedidos não pode ser nulo");
    }

    @Override
    public Pedido executar(UUID pedidoId, String produtoId, int quantidade, BigDecimal precoUnitario) {
        Objects.requireNonNull(pedidoId, "pedidoId não pode ser nulo");

        Pedido pedidoExistente = pedidos.porId(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado para o ID: " + pedidoId));

        ItemPedido novoItem = new ItemPedido(produtoId, quantidade, precoUnitario);
        Pedido pedidoAtualizado = pedidoExistente.adicionarItem(novoItem);

        return pedidos.salvar(pedidoAtualizado);
    }
}
