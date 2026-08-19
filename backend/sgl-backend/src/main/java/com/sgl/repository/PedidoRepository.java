package com.sgl.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    Optional<Pedido> findByPublicId(UUID publicId);

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

    // Busca pedidos de um projeto dentro do laboratório e do período informados.
    @Query("""
            SELECT pedido
            FROM Pedido pedido
            WHERE pedido.laboratorio.id = :laboratorioId
              AND pedido.projeto.id = :projetoId
              AND pedido.dataSolicitacao BETWEEN :dataInicio AND :dataFim
            ORDER BY pedido.dataSolicitacao ASC, pedido.id ASC
            """)
    List<Pedido> findByLaboratorioProjetoEPeriodo(
            @Param("laboratorioId") Long laboratorioId,
            @Param("projetoId") Long projetoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );
}
