package br.com.pedidos.api.application.exception;

public class ResultadoPagamentoInvalidoException extends RuntimeException {

    public ResultadoPagamentoInvalidoException(String message) {
        super(message);
    }
}