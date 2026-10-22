package br.com.pedidos.api.config;

import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.service.AdicionarItemService;
import br.com.pedidos.api.application.service.CriarPedidoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CasosDeUsoConfig {

    @Bean
    public CriarPedido criarPedido(Pedidos pedidos) {
        return new CriarPedidoService(pedidos);
    }

    @Bean
    public AdicionarItem adicionarItem(Pedidos pedidos) {
        return new AdicionarItemService(pedidos);
    }
}
