package br.com.pedidos.api.application.port.in;

import br.com.pedidos.api.application.model.ResultadoPagamento;

public interface AplicarResultadoPagamento {

    void executar(ResultadoPagamento resultado);
}