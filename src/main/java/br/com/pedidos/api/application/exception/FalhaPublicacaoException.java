package br.com.pedidos.api.application.exception;

public class FalhaPublicacaoException extends RuntimeException {

    public FalhaPublicacaoException(String message) {
        super(message);
    }

    public FalhaPublicacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}