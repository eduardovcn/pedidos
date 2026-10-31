package br.com.pedidos.api.application.service;

import br.com.pedidos.api.application.exception.PedidoNaoEncontradoException;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.Pedido;

import java.util.Objects;
import java.util.UUID;

public class ConsultarPedidoService implements ConsultarPedido {

    private final Pedidos pedidos;

    public ConsultarPedidoService(Pedidos pedidos) {
        this.pedidos = Objects.requireNonNull(pedidos, "pedidos não pode ser nulo");
    }

    @Override
    public Pedido executar(UUID pedidoId) {
        Objects.requireNonNull(pedidoId, "pedidoId não pode ser nulo");
        return pedidos.porId(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado para o ID: " + pedidoId));
    }
}