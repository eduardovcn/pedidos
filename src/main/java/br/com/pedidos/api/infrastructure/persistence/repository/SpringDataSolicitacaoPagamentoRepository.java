package br.com.pedidos.api.infrastructure.persistence.repository;

import br.com.pedidos.api.infrastructure.persistence.entity.SolicitacaoPagamentoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataSolicitacaoPagamentoRepository extends JpaRepository<SolicitacaoPagamentoJpaEntity, UUID> {
}