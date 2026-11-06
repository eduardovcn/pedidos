package br.com.pagamentos.application.service;

import br.com.pagamentos.application.exception.DecisaoConflitanteException;
import br.com.pagamentos.application.exception.SolicitacaoInvalidaException;
import br.com.pagamentos.application.port.in.ProcessarSolicitacaoPagamento;
import br.com.pagamentos.application.port.out.Decisoes;
import br.com.pagamentos.application.port.out.PublicadorResultado;
import br.com.pagamentos.domain.CenarioPagamento;
import br.com.pagamentos.domain.DecisaoPagamento;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class ProcessarSolicitacaoPagamentoService implements ProcessarSolicitacaoPagamento {

    private static final int CASAS_DECIMAIS = 2;

    private final Decisoes decisoes;
    private final PublicadorResultado publicador;

    public ProcessarSolicitacaoPagamentoService(Decisoes decisoes, PublicadorResultado publicador) {
        this.decisoes = Objects.requireNonNull(decisoes, "decisoes não pode ser nulo");
        this.publicador = Objects.requireNonNull(publicador, "publicador não pode ser nulo");
    }

    @Override
    public void executar(UUID solicitacaoId, UUID pedidoId, BigDecimal valor, String cenarioTexto) {
        if (solicitacaoId == null) {
            throw new SolicitacaoInvalidaException("solicitacaoId é obrigatório");
        }
        if (pedidoId == null) {
            throw new SolicitacaoInvalidaException("pedidoId é obrigatório");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new SolicitacaoInvalidaException("valor deve ser positivo");
        }
        if (valor.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw new SolicitacaoInvalidaException("valor admite no máximo " + CASAS_DECIMAIS + " casas decimais");
        }
        CenarioPagamento cenario = interpretar(cenarioTexto);

        DecisaoPagamento decisao = decisoes.porId(solicitacaoId).orElse(null);
        if (decisao == null) {
            decisao = decisoes.salvarSeAusente(DecisaoPagamento.decidir(solicitacaoId, pedidoId, valor, cenario));
        }
        if (!decisao.corresponde(pedidoId, valor, cenario)) {
            throw new DecisaoConflitanteException(
                    "solicitacaoId " + solicitacaoId + " já foi decidido com outro pedido, valor ou cenário");
        }

        publicador.publicar(decisao);
    }

    private static CenarioPagamento interpretar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new SolicitacaoInvalidaException("cenario é obrigatório");
        }
        try {
            return CenarioPagamento.valueOf(texto);
        } catch (IllegalArgumentException e) {
            throw new SolicitacaoInvalidaException("cenario desconhecido: " + texto);
        }
    }
}
