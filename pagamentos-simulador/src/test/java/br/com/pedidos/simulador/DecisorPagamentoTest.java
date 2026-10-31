package br.com.pedidos.simulador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DecisorPagamentoTest {

    private SolicitacaoPagamentoMensagem solicitacao(String cenario) {
        return new SolicitacaoPagamentoMensagem(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("37.80"), cenario);
    }

    @Test
    @DisplayName("APROVAR gera aprovado=true com os mesmos IDs e valor")
    void aprova() {
        var s = solicitacao("APROVAR");
        var r = DecisorPagamento.decidir(s);

        assertTrue(r.aprovado());
        assertEquals(DecisorPagamento.MOTIVO_APROVADO, r.motivo());
        assertEquals(s.solicitacaoId(), r.solicitacaoId());
        assertEquals(s.pedidoId(), r.pedidoId());
        assertEquals(s.valor(), r.valor());
    }

    @Test
    @DisplayName("RECUSAR gera aprovado=false")
    void recusa() {
        var r = DecisorPagamento.decidir(solicitacao("RECUSAR"));

        assertFalse(r.aprovado());
        assertEquals(DecisorPagamento.MOTIVO_RECUSADO, r.motivo());
    }

    @Test
    @DisplayName("A mesma solicitação sempre produz o mesmo resultado (idempotente)")
    void deterministico() {
        var s = solicitacao("RECUSAR");

        assertEquals(DecisorPagamento.decidir(s), DecisorPagamento.decidir(s));
    }

    @Test
    @DisplayName("Cenário desconhecido ou solicitação incompleta é rejeitado")
    void invalidos() {
        assertThrows(IllegalArgumentException.class, () -> DecisorPagamento.decidir(solicitacao("TALVEZ")));
        assertThrows(IllegalArgumentException.class, () -> DecisorPagamento.decidir(
                new SolicitacaoPagamentoMensagem(null, UUID.randomUUID(), BigDecimal.TEN, "APROVAR")));
    }
}