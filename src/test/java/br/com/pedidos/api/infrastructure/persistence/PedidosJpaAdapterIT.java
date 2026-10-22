package br.com.pedidos.api.infrastructure.persistence;

import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import br.com.pedidos.api.domain.StatusPedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PedidosJpaAdapterIT {

    @Autowired
    private PedidosJpaAdapter adapter;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Deve salvar pedido de c-1 com CAFE-500, 2 x 18.90 e recuperar por UUID em nova transação")
    void deveSalvarERecuperarPedidoEmNovaTransacao() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        // 1. Salva o pedido em uma transação dedicada
        Pedido pedidoSalvo = tx.execute(status -> {
            Pedido pedido = Pedido.novo("c-1")
                    .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90")));
            return adapter.salvar(pedido);
        });

        assertNotNull(pedidoSalvo);
        var pedidoId = pedidoSalvo.id();

        // 2. Recupera por UUID em uma NOVA transação independente
        Pedido pedidoRecuperado = tx.execute(status -> {
            var opt = adapter.porId(pedidoId);
            assertTrue(opt.isPresent(), "Pedido deveria existir no banco de dados");
            return opt.get();
        });

        // 3. Verificações do domínio recuperado
        assertNotNull(pedidoRecuperado);
        assertEquals(pedidoId, pedidoRecuperado.id());
        assertEquals("c-1", pedidoRecuperado.clienteId());
        assertEquals(StatusPedido.ABERTO, pedidoRecuperado.status());
        assertEquals(1, pedidoRecuperado.itens().size());

        ItemPedido item = pedidoRecuperado.itens().get(0);
        assertEquals("CAFE-500", item.produtoId());
        assertEquals(2, item.quantidade());
        assertEquals(new BigDecimal("18.90"), item.precoUnitario());
        assertEquals(new BigDecimal("37.80"), item.subtotal());

        // Total derivado
        assertEquals(new BigDecimal("37.80"), pedidoRecuperado.total());
    }
}
