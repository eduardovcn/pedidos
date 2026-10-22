package br.com.pedidos.api.infrastructure.persistence.entity;

import br.com.pedidos.api.domain.StatusPedido;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class PedidoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemJpaEntity> itens = new ArrayList<>();

    protected PedidoJpaEntity() {
    }

    public PedidoJpaEntity(UUID id, String clienteId, StatusPedido status) {
        this.id = id;
        this.clienteId = clienteId;
        this.status = status;
    }

    public void adicionarItem(ItemJpaEntity item) {
        itens.add(item);
        item.setPedido(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public List<ItemJpaEntity> getItens() {
        return itens;
    }

    public void setItens(List<ItemJpaEntity> itens) {
        this.itens = itens;
    }
}
