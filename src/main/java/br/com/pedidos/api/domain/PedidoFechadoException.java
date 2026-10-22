package br.com.pedidos.api.domain;

public class PedidoFechadoException extends IllegalStateException {

    public PedidoFechadoException(String message) {
        super(message);
    }
}
