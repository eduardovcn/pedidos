package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.domain.ItemPedido;
import br.com.pedidos.api.domain.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerIT {

    private static final String ITEM_VALIDO = """
            {"produtoId":"CAFE-500","quantidade":2,"precoUnitario":18.90}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Pedidos pedidos;

    @Autowired
    private JdbcTemplate jdbc;

    private long linhas(String tabela) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
    }

    private Pedido pedidoAbertoPersistido() {
        return pedidos.salvar(Pedido.novo("c-1")
                .adicionarItem(new ItemPedido("CAFE-500", 2, new BigDecimal("18.90"))));
    }

    @Test
    @DisplayName("POST /pedidos retorna 201, Location e total 37.80")
    void criaPedido() throws Exception {
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"clienteId":"c-1","itens":[%s]}""".formatted(ITEM_VALIDO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/pedidos/")))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.total").value(37.80));
    }

    @Test
    @DisplayName("POST /pedidos com quantidade 0 retorna 422 sem gravar linhas")
    void quantidadeZero() throws Exception {
        long p = linhas("pedido"), i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"clienteId":"c-1","itens":[{"produtoId":"CAFE-500","quantidade":0,"precoUnitario":18.90}]}"""))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(p, linhas("pedido"));
        assertEquals(i, linhas("item_pedido"));
    }

    @Test
    @DisplayName("POST /pedidos com itens vazios retorna 422 sem gravar linhas")
    void listaVazia() throws Exception {
        long p = linhas("pedido"), i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":\"c-1\",\"itens\":[]}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(p, linhas("pedido"));
        assertEquals(i, linhas("item_pedido"));
    }

    @Test
    @DisplayName("POST /pedidos com JSON malformado retorna 400 sem gravar linhas")
    void jsonMalformado() throws Exception {
        long p = linhas("pedido"), i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON).content("{ invalido"))
                .andExpect(status().isBadRequest());
        assertEquals(p, linhas("pedido"));
        assertEquals(i, linhas("item_pedido"));
    }

    @Test
    @DisplayName("POST /pedidos/{id}/itens retorna 200 com 2 itens e total 56.70")
    void adicionaItem() throws Exception {
        Pedido existente = pedidoAbertoPersistido();
        mockMvc.perform(post("/pedidos/{id}/itens", existente.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":\"CAFE-500\",\"quantidade\":1,\"precoUnitario\":18.90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existente.id().toString()))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.total").value(56.70));
    }

    @Test
    @DisplayName("POST /pedidos/{id}/itens com id inexistente retorna 404 sem gravar linhas")
    void pedidoInexistente() throws Exception {
        long i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos/{id}/itens", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM_VALIDO))
                .andExpect(status().isNotFound());
        assertEquals(i, linhas("item_pedido"));
    }

    @Test
    @DisplayName("POST /pedidos/{id}/itens em pedido PAGO retorna 409 sem gravar linhas")
    void pedidoFechado() throws Exception {
        Pedido pago = pedidos.salvar(pedidoAbertoPersistido().pagar());
        long i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos/{id}/itens", pago.id()).contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM_VALIDO))
                .andExpect(status().isConflict());
        assertEquals(i, linhas("item_pedido"));
    }

    @Test
    @DisplayName("POST /pedidos/{id}/itens com quantidade 0 retorna 422 sem gravar linhas")
    void itemInvalido() throws Exception {
        Pedido existente = pedidoAbertoPersistido();
        long i = linhas("item_pedido");
        mockMvc.perform(post("/pedidos/{id}/itens", existente.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":\"CAFE-500\",\"quantidade\":0,\"precoUnitario\":18.90}"))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(i, linhas("item_pedido"));
    }
}
