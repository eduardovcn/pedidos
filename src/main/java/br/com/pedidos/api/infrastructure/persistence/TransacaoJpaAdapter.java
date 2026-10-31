package br.com.pedidos.api.infrastructure.persistence;

import br.com.pedidos.api.application.port.out.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransacaoJpaAdapter implements Transacao {

    private final TransactionTemplate template;

    public TransacaoJpaAdapter(PlatformTransactionManager manager) {
        this.template = new TransactionTemplate(manager);
    }

    @Override
    public <T> T executar(Supplier<T> trabalho) {
        return template.execute(status -> trabalho.get());
    }
}