package br.com.pagamentos.config;

import br.com.pagamentos.application.port.in.ProcessarSolicitacaoPagamento;
import br.com.pagamentos.application.port.out.Decisoes;
import br.com.pagamentos.application.port.out.PublicadorResultado;
import br.com.pagamentos.application.service.ProcessarSolicitacaoPagamentoService;
import br.com.pagamentos.infrastructure.messaging.PublicadorResultadoAmqp;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static br.com.pagamentos.infrastructure.messaging.PagamentosMensageria.CHAVE_RESULTADO;
import static br.com.pagamentos.infrastructure.messaging.PagamentosMensageria.EXCHANGE_EVENTOS;

/** Liga o núcleo (puro) aos adaptadores. */
@Configuration
public class CasosDeUsoConfig {

    @Bean
    PublicadorResultado publicadorResultado(RabbitTemplate rabbitTemplate) {
        return new PublicadorResultadoAmqp(rabbitTemplate, EXCHANGE_EVENTOS, CHAVE_RESULTADO, Duration.ofSeconds(5));
    }

    @Bean
    ProcessarSolicitacaoPagamento processarSolicitacaoPagamento(Decisoes decisoes, PublicadorResultado publicador) {
        return new ProcessarSolicitacaoPagamentoService(decisoes, publicador);
    }
}
