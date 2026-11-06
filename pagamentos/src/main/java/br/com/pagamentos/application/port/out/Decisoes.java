package br.com.pagamentos.application.port.out;

import br.com.pagamentos.domain.DecisaoPagamento;

import java.util.Optional;
import java.util.UUID;

public interface Decisoes {

    Optional<DecisaoPagamento> porId(UUID solicitacaoId);

    /** Grava a decisão. Se outra já existir para o mesmo solicitacaoId, devolve a existente. */
    DecisaoPagamento salvarSeAusente(DecisaoPagamento decisao);
}
