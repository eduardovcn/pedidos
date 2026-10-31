package br.com.pedidos.api.config;

import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.infrastructure.messaging.PagamentosMensageria;
import br.com.pedidos.api.infrastructure.messaging.PublicadorSolicitacaoPagamentoAmqp;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MensageriaConfig {

    @Bean
    public PublicadorSolicitacaoPagamento publicadorSolicitacaoPagamento(
            RabbitTemplate rabbitTemplate,
            @Value("${pagamentos.publicacao.timeout-confirmacao:5s}") Duration timeoutConfirmacao) {
        return new PublicadorSolicitacaoPagamentoAmqp(rabbitTemplate,
                PagamentosMensageria.EXCHANGE_COMANDOS, PagamentosMensageria.CHAVE_SOLICITAR, timeoutConfirmacao);
    }
}