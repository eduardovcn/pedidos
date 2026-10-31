package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.exception.FalhaPublicacaoException;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PagamentoFalhaPublicacaoIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Pedidos pedidos;
    @MockitoBean
    private PublicadorSolicitacaoPagamento publicador;

    @Test
    @DisplayName("Falha de publicação: 503 sem 202; reenvio com o mesmo solicitacaoId republica e responde 202")
    void falhaERecuperacao() throws Exception {
        Pedido pedido = pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
        UUID solicitacaoId = UUID.randomUUID();
        String corpo = "{\"solicitacaoId\":\"%s\",\"cenario\":\"APROVAR\"}".formatted(solicitacaoId);

        Mockito.doThrow(new FalhaPublicacaoException("sem confirmação")).when(publicador).publicar(Mockito.any());
        mockMvc.perform(post("/pedidos/{id}/pagamentos", pedido.id())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(get("/pedidos/{id}", pedido.id()))
                .andExpect(jsonPath("$.status").value("EM_PAGAMENTO"));

        Mockito.doNothing().when(publicador).publicar(Mockito.any());
        mockMvc.perform(post("/pedidos/{id}/pagamentos", pedido.id())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.situacao").value("PUBLICADA"));
        Mockito.verify(publicador, Mockito.times(2)).publicar(Mockito.any());
    }
}