package br.com.pedidos.api.infrastructure.messaging;

import br.com.pedidos.api.application.model.ResultadoPagamento;
import br.com.pedidos.api.application.exception.ResultadoPagamentoInvalidoException;
import br.com.pedidos.api.application.port.in.AplicarResultadoPagamento;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ResultadoPagamentoListener {

    private final AplicarResultadoPagamento aplicarResultado;

    public ResultadoPagamentoListener(AplicarResultadoPagamento aplicarResultado) {
        this.aplicarResultado = Objects.requireNonNull(aplicarResultado);
    }

    @RabbitListener(queues = PagamentosMensageria.FILA_RESULTADOS)
    public void receber(ResultadoPagamentoMensagem m,
                        @Header(name = PagamentosMensageria.HEADER_VERSAO, required = false) Integer versao) {
        if (versao == null || versao != PagamentosMensageria.VERSAO) {
            throw new AmqpRejectAndDontRequeueException("Versão de mensagem não suportada: " + versao);
        }
        if (m == null || m.solicitacaoId() == null || m.pedidoId() == null || m.valor() == null || m.aprovado() == null) {
            throw new AmqpRejectAndDontRequeueException("Resultado de pagamento incompleto");
        }
        try {
            aplicarResultado.executar(new ResultadoPagamento(
                    m.solicitacaoId(), m.pedidoId(), m.valor(), m.aprovado(), m.motivo()));
        } catch (ResultadoPagamentoInvalidoException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}