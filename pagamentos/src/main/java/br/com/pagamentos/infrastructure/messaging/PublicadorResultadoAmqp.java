package br.com.pagamentos.infrastructure.messaging;

import br.com.pagamentos.application.exception.FalhaPublicacaoException;
import br.com.pagamentos.application.port.out.PublicadorResultado;
import br.com.pagamentos.domain.DecisaoPagamento;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Publica com publisher confirms + mandatory. Só retorna com ack do broker e sem devolução (return). */
public class PublicadorResultadoAmqp implements PublicadorResultado {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final Duration timeoutConfirmacao;

    public PublicadorResultadoAmqp(RabbitTemplate rabbitTemplate, String exchange, String routingKey,
                                   Duration timeoutConfirmacao) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate);
        this.exchange = Objects.requireNonNull(exchange);
        this.routingKey = Objects.requireNonNull(routingKey);
        this.timeoutConfirmacao = Objects.requireNonNull(timeoutConfirmacao);
    }

    @Override
    public void publicar(DecisaoPagamento d) {
        var mensagem = new ResultadoPagamentoMensagem(d.solicitacaoId(), d.pedidoId(), d.valor(), d.aprovado(),
                d.motivo());
        var correlacao = new CorrelationData(d.solicitacaoId().toString());

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, mensagem, mp -> {
                mp.getMessageProperties().setHeader(PagamentosMensageria.HEADER_VERSAO, PagamentosMensageria.VERSAO);
                mp.getMessageProperties().setMessageId(d.solicitacaoId().toString());
                return mp;
            }, correlacao);

            var confirmacao = correlacao.getFuture().get(timeoutConfirmacao.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirmacao.ack()) {
                throw new FalhaPublicacaoException("Broker recusou (nack) o resultado " + d.solicitacaoId()
                        + ": " + confirmacao.reason());
            }
            if (correlacao.getReturned() != null) {
                throw new FalhaPublicacaoException("Resultado devolvido pelo broker (sem rota) para "
                        + d.solicitacaoId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FalhaPublicacaoException("Publicação interrompida", e);
        } catch (ExecutionException | TimeoutException | AmqpException e) {
            throw new FalhaPublicacaoException("Sem confirmação do broker para o resultado " + d.solicitacaoId(), e);
        }
    }
}
