package br.com.pedidos.api.infrastructure.messaging;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.domain.CenarioPagamento;
import br.com.pedidos.api.domain.SolicitacaoPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static br.com.pedidos.api.infrastructure.messaging.PagamentosMensageria.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PublicadorSolicitacaoPagamentoAmqpIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    private SolicitacaoPagamento solicitacao() {
        return SolicitacaoPagamento.nova(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"), CenarioPagamento.APROVAR);
    }

    @Test
    @DisplayName("Rota inexistente: mensagem devolvida pelo broker vira FalhaPublicacaoException")
    void rotaInexistente() {
        var publicador = new PublicadorSolicitacaoPagamentoAmqp(
                rabbitTemplate, EXCHANGE_COMANDOS, "chave.inexistente", Duration.ofSeconds(5));

        assertThrows(FalhaPublicacaoException.class, () -> publicador.publicar(solicitacao()));
    }

    @Test
    @DisplayName("Exchange inexistente: nack do broker vira FalhaPublicacaoException")
    void exchangeInexistente() {
        var publicador = new PublicadorSolicitacaoPagamentoAmqp(
                rabbitTemplate, "exchange.inexistente", CHAVE_SOLICITAR, Duration.ofSeconds(5));

        assertThrows(FalhaPublicacaoException.class, () -> publicador.publicar(solicitacao()));
    }

    @Test
    @DisplayName("Rota válida: confirmação com ack e mensagem na fila")
    void rotaValida() {
        rabbitAdmin.purgeQueue(FILA_SOLICITACOES, false);
        var publicador = new PublicadorSolicitacaoPagamentoAmqp(
                rabbitTemplate, EXCHANGE_COMANDOS, CHAVE_SOLICITAR, Duration.ofSeconds(5));

        assertDoesNotThrow(() -> publicador.publicar(solicitacao()));
        assertNotNull(rabbitTemplate.receive(FILA_SOLICITACOES, 5000));
    }
}