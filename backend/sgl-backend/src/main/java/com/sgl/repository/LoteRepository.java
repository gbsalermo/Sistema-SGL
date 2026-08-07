package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Lote;

import jakarta.persistence.LockModeType;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

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

    /**
     * FEFO: para produtos perecíveis, prioriza o lote com validade mais próxima.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
              AND lote.dataValidade IS NOT NULL
            ORDER BY lote.dataValidade ASC, lote.dataEntrada ASC, lote.id ASC
            """)
    List<Lote> buscarDisponiveisPorFefoComBloqueio(
            @Param("estoqueId") Long estoqueId
    );

    /**
     * FIFO: para produtos não perecíveis, prioriza o lote que entrou primeiro.
     */
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
}
