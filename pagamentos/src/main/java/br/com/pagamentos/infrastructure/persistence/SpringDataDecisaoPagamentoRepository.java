package br.com.pagamentos.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDecisaoPagamentoRepository extends JpaRepository<DecisaoPagamentoJpaEntity, UUID> {
}
