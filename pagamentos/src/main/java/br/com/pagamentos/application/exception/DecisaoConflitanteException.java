package br.com.pagamentos.application.exception;

/** Mesmo solicitacaoId recebido com outro pedido, valor ou cenário. */
public class DecisaoConflitanteException extends IllegalStateException {
    public DecisaoConflitanteException(String mensagem) {
        super(mensagem);
    }
}
