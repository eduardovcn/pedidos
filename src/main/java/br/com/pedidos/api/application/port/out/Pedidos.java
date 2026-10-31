package br.com.pedidos.api.application.port.out;

import br.com.pedidos.api.domain.Pedido;

import java.util.Optional;
import java.util.UUID;

public interface Pedidos {

    Pedido salvar(Pedido pedido);

    Optional<Pedido> porId(UUID id);

    /** Busca com bloqueio para evitar corridas em transições de estado. Por padrão, igual a {@link #porId}. */
    default Optional<Pedido> porIdParaAtualizacao(UUID id) {
        return porId(id);
    }
}
