package br.com.pedidos.api.domain;

public class ItemInvalidoException extends IllegalArgumentException {

    public ItemInvalidoException(String message) {
        super(message);
    }
}
