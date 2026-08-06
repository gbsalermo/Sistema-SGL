package com.sgl.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Pedido;
import com.sgl.model.enums.StatusPedido;

import jakarta.persistence.LockModeType;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /*
     * Busca o pedido com bloqueio pessimista de escrita.
     *
     * Deve ser usada nas operações que alteram o status do pedido, impedindo
     * que duas transações processem simultaneamente o mesmo estado anterior.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pedido
            FROM Pedido pedido
            WHERE pedido.id = :id
            """)
    Optional<Pedido> buscarPorIdComBloqueio(@Param("id") Long id);

    List<Pedido> findByUsuarioId(Long usuarioId);

    List<Pedido> findByLaboratorioId(Long laboratorioId);

    List<Pedido> findByStatus(StatusPedido status);

    List<Pedido> findByLaboratorioIdAndStatus(
            Long laboratorioId,
            StatusPedido status
    );
}
