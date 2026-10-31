package br.com.pedidos.api.infrastructure.messaging;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica com publisher confirms + mandatory. Só retorna normalmente com ack do broker e sem devolução (return).
 */
public class PublicadorSolicitacaoPagamentoAmqp implements PublicadorSolicitacaoPagamento {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final Duration timeoutConfirmacao;

    public PublicadorSolicitacaoPagamentoAmqp(RabbitTemplate rabbitTemplate, String exchange, String routingKey,
                                              Duration timeoutConfirmacao) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
        this.exchange = Objects.requireNonNull(exchange);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.timeoutConfirmacao = Objects.requireNonNull(timeoutConfirmacao);
    }

    @Override
    public void publicar(SolicitacaoPagamento s) {
        var mensagem = new SolicitacaoPagamentoMensagem(s.solicitacaoId(), s.pedidoId(), s.valor(), s.cenario().name());
        var correlacao = new CorrelationData(s.solicitacaoId().toString());

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, mensagem, mp -> {
                mp.getMessageProperties().setHeader(PagamentosMensageria.HEADER_VERSAO, PagamentosMensageria.VERSAO);
                mp.getMessageProperties().setMessageId(s.solicitacaoId().toString());
                return mp;
            }, correlacao);

            var confirmacao = correlacao.getFuture().get(timeoutConfirmacao.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirmacao.ack()) {
                throw new FalhaPublicacaoException("Broker recusou (nack) a solicitação " + s.solicitacaoId()
                        + ": " + confirmacao.reason());
            }
            if (correlacao.getReturned() != null) {
                throw new FalhaPublicacaoException("Mensagem devolvida pelo broker (sem rota) para a solicitação "
                        + s.solicitacaoId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaPublicacaoException("Publicação interrompida", e);
        } catch (ExecutionException | TimeoutException | AmqpException e) {
            throw new FalhaPublicacaoException("Sem confirmação do broker para a solicitação " + s.solicitacaoId(), e);
        }
    }
}