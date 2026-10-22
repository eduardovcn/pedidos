package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.domain.Pedido;

import java.math.BigDecimal;
import java.util.UUID;

public interface AdicionarItem {

    Pedido executar(UUID pedidoId, String produtoId, int quantidade, BigDecimal precoUnitario);
}
