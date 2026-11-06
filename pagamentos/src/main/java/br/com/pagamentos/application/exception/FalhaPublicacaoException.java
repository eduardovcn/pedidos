package br.com.pagamentos.application.exception;

/** O resultado não foi confirmado pelo broker ou não tem rota. */
public class FalhaPublicacaoException extends RuntimeException {
    public FalhaPublicacaoException(String mensagem) {
        super(mensagem);
    }

    public FalhaPublicacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
