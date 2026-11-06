package br.com.pagamentos.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessarSolicitacaoPagamento {

    /**
     * Salva a decisão (uma só por solicitacaoId) e publica o resultado. Só retorna normalmente depois de
     * salvar e de o resultado ser confirmado e roteado.
     */
    void executar(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, String cenario);
}
