package br.com.pagamentos.application.exception;

/** Solicitação malformada (IDs, valor ou cenário inválidos). Não adianta tentar de novo. */
public class SolicitacaoInvalidaException extends IllegalArgumentException {
    public SolicitacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
