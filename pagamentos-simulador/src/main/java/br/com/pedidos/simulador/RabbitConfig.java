package br.com.pedidos.simulador;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static br.com.pedidos.simulador.Mensageria.*;

/**
 * Declara a mesma topologia do serviço de pedidos (idêntica, para que a ordem de subida não importe).
 * Os argumentos das filas principais não podem divergir da declaração original.
 */
@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange pagamentosComandos() {
        return new DirectExchange(EXCHANGE_COMANDOS, true, false);
    }

    @Bean
    DirectExchange pagamentosEventos() {
        return new DirectExchange(EXCHANGE_EVENTOS, true, false);
    }

    @Bean
    Queue pagamentosSolicitacoesErros() {
        return QueueBuilder.durable(FILA_SOLICITACOES_ERROS).build();
    }

    @Bean
    Queue pedidosResultadosErros() {
        return QueueBuilder.durable(FILA_RESULTADOS_ERROS).build();
    }

    @Bean
    Queue pagamentosSolicitacoes() {
        return QueueBuilder.durable(FILA_SOLICITACOES)
                .deadLetterExchange("")
                .deadLetterRoutingKey(FILA_SOLICITACOES_ERROS)
                .build();
    }

    @Bean
    Queue pedidosResultados() {
        return QueueBuilder.durable(FILA_RESULTADOS)
                .deadLetterExchange("")
                .deadLetterRoutingKey(FILA_RESULTADOS_ERROS)
                .build();
    }

    @Bean
    Binding bindingSolicitacoes(Queue pagamentosSolicitacoes, DirectExchange pagamentosComandos) {
        return BindingBuilder.bind(pagamentosSolicitacoes).to(pagamentosComandos).with(CHAVE_SOLICITAR);
    }

    @Bean
    Binding bindingResultados(Queue pedidosResultados, DirectExchange pagamentosEventos) {
        return BindingBuilder.bind(pedidosResultados).to(pagamentosEventos).with(CHAVE_RESULTADO);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        var conversor = new JacksonJsonMessageConverter();
        conversor.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        return conversor;
    }
}