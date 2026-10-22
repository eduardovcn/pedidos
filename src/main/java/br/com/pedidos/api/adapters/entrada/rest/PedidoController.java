package br.com.pedidos.api.adapters.entrada.rest;

import br.com.pedidos.api.adapters.entrada.rest.dto.ItemRequest;
import br.com.pedidos.api.adapters.entrada.rest.dto.PedidoRequest;
import br.com.pedidos.api.adapters.entrada.rest.dto.PedidoResponse;
import br.com.pedidos.api.application.port.in.AdicionarItem;
import br.com.pedidos.api.application.port.in.CriarPedido;
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

    public PedidoController(CriarPedido criarPedido, AdicionarItem adicionarItem) {
        this.criarPedido = Objects.requireNonNull(criarPedido, "criarPedido não pode ser nulo");
        this.adicionarItem = Objects.requireNonNull(adicionarItem, "adicionarItem não pode ser nulo");
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
