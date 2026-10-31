package br.com.pedidos.simulador;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static br.com.pedidos.simulador.Mensageria.*;

@Component
public class SolicitacaoPagamentoListener {

    private final RabbitTemplate rabbitTemplate;

    public SolicitacaoPagamentoListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = FILA_SOLICITACOES)
    public void receber(SolicitacaoPagamentoMensagem solicitacao,
                        @Header(name = HEADER_VERSAO, required = false) Integer versao) throws Exception {
        if (versao == null || versao != VERSAO) {
            throw new AmqpRejectAndDontRequeueException("Versão de mensagem não suportada: " + versao);
        }
        ResultadoPagamentoMensagem resultado;
        try {
            resultado = DecisorPagamento.decidir(solicitacao);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
        publicar(resultado);
    }

    /** O ack da solicitação só ocorre depois que o resultado foi confirmado pelo broker e roteado. */
    private void publicar(ResultadoPagamentoMensagem resultado) throws Exception {
        var correlacao = new CorrelationData(resultado.solicitacaoId().toString());
        rabbitTemplate.convertAndSend(EXCHANGE_EVENTOS, CHAVE_RESULTADO, resultado, mp -> {
            mp.getMessageProperties().setHeader(HEADER_VERSAO, VERSAO);
            mp.getMessageProperties().setMessageId(resultado.solicitacaoId().toString());
            return mp;
        }, correlacao);

        var confirmacao = correlacao.getFuture().get(5, TimeUnit.SECONDS);
        if (!confirmacao.ack() || correlacao.getReturned() != null) {
            throw new IllegalStateException("Resultado não confirmado/roteado para " + resultado.solicitacaoId());
        }
    }
}