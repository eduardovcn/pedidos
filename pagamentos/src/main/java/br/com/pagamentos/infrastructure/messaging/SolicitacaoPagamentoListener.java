package br.com.pagamentos.infrastructure.messaging;

import br.com.pagamentos.application.port.in.ProcessarSolicitacaoPagamento;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static br.com.pagamentos.infrastructure.messaging.PagamentosMensageria.*;

/**
 * Consome pagamentos.solicitacoes. O ack só ocorre quando este método retorna, isto é, depois de a decisão
 * ser salva e o resultado ser confirmado e roteado. Qualquer exceção, após as tentativas limitadas
 * configuradas no application.yml, rejeita sem reenfileirar e a mensagem segue para a fila de erros.
 */
@Component
public class SolicitacaoPagamentoListener {

    private final ProcessarSolicitacaoPagamento processar;

    public SolicitacaoPagamentoListener(ProcessarSolicitacaoPagamento processar) {
        this.processar = processar;
    }

    @RabbitListener(queues = FILA_SOLICITACOES)
    public void receber(SolicitacaoPagamentoMensagem mensagem,
                        @Header(name = HEADER_VERSAO, required = false) Integer versao) {
        if (versao == null || versao != VERSAO) {
            throw new AmqpRejectAndDontRequeueException("Versão de mensagem não suportada: " + versao);
        }
        processar.executar(mensagem.solicitacaoId(), mensagem.pedidoId(), mensagem.valor(), mensagem.cenario());
    }
}
