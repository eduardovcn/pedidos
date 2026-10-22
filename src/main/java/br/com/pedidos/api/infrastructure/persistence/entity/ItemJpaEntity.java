package br.com.pedidos.api.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_pedido")
public class ItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemJpaEntity() {
    }

    public ItemJpaEntity(String sku, int quantidade, BigDecimal precoUnitario) {
        this.sku = sku;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PedidoJpaEntity getPedido() {
        return pedido;
    }

    public void setPedido(PedidoJpaEntity pedido) {
        this.pedido = pedido;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
