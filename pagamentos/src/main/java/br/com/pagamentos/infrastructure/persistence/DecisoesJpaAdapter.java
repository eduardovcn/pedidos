package br.com.pagamentos.infrastructure.persistence;

import br.com.pagamentos.application.port.out.Decisoes;
import br.com.pagamentos.domain.DecisaoPagamento;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DecisoesJpaAdapter implements Decisoes {

    private final SpringDataDecisaoPagamentoRepository repositorio;

    public DecisoesJpaAdapter(SpringDataDecisaoPagamentoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<DecisaoPagamento> porId(UUID solicitacaoId) {
        return repositorio.findById(solicitacaoId).map(DecisaoPagamentoJpaEntity::paraDominio);
    }

    /**
     * O insert é feito em transação própria do repositório. Se outro processamento gravar a mesma chave antes,
     * a restrição única rejeita este insert e a decisão já gravada é reutilizada.
     */
    @Override
    public DecisaoPagamento salvarSeAusente(DecisaoPagamento decisao) {
        try {
            return repositorio.saveAndFlush(DecisaoPagamentoJpaEntity.de(decisao)).paraDominio();
        } catch (DataIntegrityViolationException e) {
            return repositorio.findById(decisao.solicitacaoId())
                    .map(DecisaoPagamentoJpaEntity::paraDominio)
                    .orElseThrow(() -> e);
        }
    }
}
