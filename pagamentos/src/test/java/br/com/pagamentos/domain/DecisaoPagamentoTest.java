package br.com.pagamentos.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DecisaoPagamentoTest {

    private final UUID solicitacao = UUID.randomUUID();
    private final UUID pedido = UUID.randomUUID();

    @Test
    @DisplayName("APROVAR gera aprovado=true e RECUSAR gera aprovado=false, sempre o mesmo resultado")
    void determinismo() {
        var a1 = DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("37.80"), CenarioPagamento.APROVAR);
        var a2 = DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("37.80"), CenarioPagamento.APROVAR);
        var r = DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("37.80"), CenarioPagamento.RECUSAR);

        assertTrue(a1.aprovado());
        assertEquals("APROVADO_SIMULADO", a1.motivo());
        assertEquals(a1, a2);
        assertFalse(r.aprovado());
        assertEquals("RECUSADO_SIMULADO", r.motivo());
    }

    @Test
    @DisplayName("Preserva IDs e valor")
    void preservaDados() {
        var d = DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("37.80"), CenarioPagamento.APROVAR);

        assertEquals(solicitacao, d.solicitacaoId());
        assertEquals(pedido, d.pedidoId());
        assertEquals(new BigDecimal("37.80"), d.valor());
    }

    @Test
    @DisplayName("Valor zero ou negativo é rejeitado")
    void valorPositivo() {
        assertThrows(IllegalArgumentException.class,
                () -> DecisaoPagamento.decidir(solicitacao, pedido, BigDecimal.ZERO, CenarioPagamento.APROVAR));
        assertThrows(IllegalArgumentException.class,
                () -> DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("-1"), CenarioPagamento.APROVAR));
    }

    @Test
    @DisplayName("corresponde compara valor por compareTo")
    void correspondeUsaCompareTo() {
        var d = DecisaoPagamento.decidir(solicitacao, pedido, new BigDecimal("37.80"), CenarioPagamento.APROVAR);

        assertTrue(d.corresponde(pedido, new BigDecimal("37.8"), CenarioPagamento.APROVAR));
        assertFalse(d.corresponde(UUID.randomUUID(), new BigDecimal("37.80"), CenarioPagamento.APROVAR));
        assertFalse(d.corresponde(pedido, new BigDecimal("37.81"), CenarioPagamento.APROVAR));
        assertFalse(d.corresponde(pedido, new BigDecimal("37.80"), CenarioPagamento.RECUSAR));
    }
}
