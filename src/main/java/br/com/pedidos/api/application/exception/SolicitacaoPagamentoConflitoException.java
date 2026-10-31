package br.com.pedidos.api.application.exception;

public class SolicitacaoPagamentoConflitoException extends IllegalStateException {

    public SolicitacaoPagamentoConflitoException(String message) {
        super(message);
    }
}