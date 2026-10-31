package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.adapters.entrada.rest.dto.ItemRequest;
import br.com.pedidos.api.adapters.entrada.rest.dto.PagamentoAceitoResponse;
import br.com.pedidos.api.adapters.entrada.rest.dto.PagamentoRequest;
import br.com.pedidos.api.adapters.entrada.rest.dto.PedidoRequest;
import br.com.pedidos.api.adapters.entrada.rest.dto.PedidoResponse;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.ConsultarPedido;
import br.com.pedidos.api.application.port.in.CriarPedido;
import br.com.pedidos.api.application.port.in.SolicitarPagamento;
import br.com.pedidos.api.domain.Pedido;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final CriarPedido criarPedido;
    private final AdicionarItem adicionarItem;
    private final ConsultarPedido consultarPedido;
    private final SolicitarPagamento solicitarPagamento;

    public PedidoController(CriarPedido criarPedido, AdicionarItem adicionarItem,
                            ConsultarPedido consultarPedido, SolicitarPagamento solicitarPagamento) {
        this.criarPedido = Objects.requireNonNull(criarPedido, "criarPedido não pode ser nulo");
        this.adicionarItem = Objects.requireNonNull(adicionarItem, "adicionarItem não pode ser nulo");
        this.consultarPedido = Objects.requireNonNull(consultarPedido, "consultarPedido não pode ser nulo");
        this.solicitarPagamento = Objects.requireNonNull(solicitarPagamento, "solicitarPagamento não pode ser nulo");
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request) {
        var itensComando = request.itens().stream()
                .map(item -> new CriarPedido.ItemComando(
                        item.produtoId(),
                        item.quantidade(),
                        item.precoUnitario()
                ))
                .toList();

        Pedido pedido = criarPedido.executar(request.clienteId(), itensComando);
        URI location = URI.create("/pedidos/" + pedido.id());

        return ResponseEntity.created(location).body(PedidoResponse.de(pedido));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(PedidoResponse.de(consultarPedido.executar(id)));
    }

    @PostMapping("/{pedidoId}/pagamentos")
    public ResponseEntity<PagamentoAceitoResponse> solicitarPagamento(
            @PathVariable UUID pedidoId,
            @Valid @RequestBody PagamentoRequest request) {
        var solicitacao = solicitarPagamento.executar(pedidoId, request.solicitacaoId(), request.cenario());
        return ResponseEntity.accepted().body(PagamentoAceitoResponse.de(solicitacao));
    }

    @PostMapping("/{id}/itens")
    public ResponseEntity<PedidoResponse> adicionarItem(
            @PathVariable UUID id,
            @Valid @RequestBody ItemRequest request) {
        Pedido pedido = adicionarItem.executar(
                id,
                request.produtoId(),
                request.quantidade(),
                request.precoUnitario()
        );

        return ResponseEntity.ok(PedidoResponse.de(pedido));
    }
}
