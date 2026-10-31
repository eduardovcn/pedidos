package br.com.pedidos.api.infrastructure.persistence.repository;

import br.com.pedidos.api.infrastructure.persistence.entity.PedidoJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPedidoRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    @Query("SELECT p FROM PedidoJpaEntity p LEFT JOIN FETCH p.itens WHERE p.id = :id")
    Optional<PedidoJpaEntity> buscarPorIdComItens(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PedidoJpaEntity p WHERE p.id = :id")
    Optional<PedidoJpaEntity> buscarPorIdComBloqueio(@Param("id") UUID id);
}
