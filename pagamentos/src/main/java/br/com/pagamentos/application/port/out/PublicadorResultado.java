package br.com.pagamentos.application.port.out;

import br.com.pagamentos.domain.DecisaoPagamento;

public interface PublicadorResultado {

    /**
     * Publica o resultado. Só retorna com confirmação do broker e roteamento verificado; caso contrário
     * lança {@link br.com.pagamentos.application.exception.FalhaPublicacaoException}.
     */
    void publicar(DecisaoPagamento decisao);
}
