package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.Pedido;

import java.math.BigDecimal;
import java.util.List;

public interface CriarPedido {

    Pedido executar(String clienteId, List<ItemComando> itens);

    record ItemComando(String produtoId, int quantidade, BigDecimal precoUnitario) {}
}
