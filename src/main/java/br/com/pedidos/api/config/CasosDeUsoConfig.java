package br.com.pedidos.api.config;

import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.AplicarResultadoPagamento;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.application.port.out.Pedidos;
import br.com.pedidos.api.application.port.out.PublicadorSolicitacaoPagamento;
import br.com.pedidos.api.application.port.out.SolicitacoesPagamento;
import br.com.pedidos.api.application.port.out.Transacao;
import br.com.pedidos.api.application.service.AdicionarItemService;
import br.com.pedidos.api.application.service.AplicarResultadoPagamentoService;
import br.com.pedidos.api.application.service.ConsultarPedidoService;
import br.com.pedidos.api.application.service.CriarPedidoService;
import br.com.pedidos.api.application.service.SolicitarPagamentoService;
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

    @Bean
    public ConsultarPedido consultarPedido(Pedidos pedidos) {
        return new ConsultarPedidoService(pedidos);
    }

    @Bean
    public SolicitarPagamento solicitarPagamento(Pedidos pedidos, SolicitacoesPagamento solicitacoes,
                                                 PublicadorSolicitacaoPagamento publicador, Transacao transacao) {
        return new SolicitarPagamentoService(pedidos, solicitacoes, publicador, transacao);
    }

    @Bean
    public AplicarResultadoPagamento aplicarResultadoPagamento(Pedidos pedidos, SolicitacoesPagamento solicitacoes,
                                                               Transacao transacao) {
        return new AplicarResultadoPagamentoService(pedidos, solicitacoes, transacao);
    }
}
