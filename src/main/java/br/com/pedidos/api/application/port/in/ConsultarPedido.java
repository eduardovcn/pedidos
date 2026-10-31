package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.Pedido;

import java.util.UUID;

public interface ConsultarPedido {

    Pedido executar(UUID pedidoId);
}