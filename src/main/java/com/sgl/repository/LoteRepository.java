package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Lote;

import jakarta.persistence.LockModeType;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
	
	Optional<Lote> findByPublicId(UUID publicId);

    List<Lote> findByEstoqueCentralId(Long estoqueCentralId);

    List<Lote> findByEstoqueCentralIdAndAtivoTrue(Long estoqueCentralId);

    Optional<Lote> findByEstoqueCentralIdAndNumeroLote(
            Long estoqueCentralId,
            String numeroLote
    );

    boolean existsByEstoqueCentralIdAndNumeroLote(
            Long estoqueCentralId,
            String numeroLote
    );

    List<Lote> findByDataValidadeBeforeAndAtivoTrue(LocalDate data);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.id = :id
            """)
    Optional<Lote> buscarPorIdComBloqueio(@Param("id") Long id);

    /** FEFO: primeiro lote válido a vencer, primeiro a sair. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
              AND lote.dataValidade IS NOT NULL
              AND lote.dataValidade >= :dataReferencia
            ORDER BY lote.dataValidade ASC, lote.dataEntrada ASC, lote.id ASC
            """)
    List<Lote> buscarDisponiveisPorFefoComBloqueio(
            @Param("estoqueId") Long estoqueId,
            @Param("dataReferencia") LocalDate dataReferencia
    );

    /** FIFO: primeiro lote recebido, primeiro a sair. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
            ORDER BY lote.dataEntrada ASC, lote.id ASC
            """)
    List<Lote> buscarDisponiveisPorEntradaComBloqueio(
            @Param("estoqueId") Long estoqueId
    );

    /** Lotes vencidos disponíveis para descarte, em ordem de vencimento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
              AND lote.dataValidade IS NOT NULL
              AND lote.dataValidade < :dataReferencia
            ORDER BY lote.dataValidade ASC, lote.dataEntrada ASC, lote.id ASC
            """)
    List<Lote> buscarVencidosComBloqueio(
            @Param("estoqueId") Long estoqueId,
            @Param("dataReferencia") LocalDate dataReferencia
    );
}
