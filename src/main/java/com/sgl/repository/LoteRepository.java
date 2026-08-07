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

    /*
     * Consulta base para FEFO.
     *
     * Retorna somente lotes ativos com saldo positivo, ordenando primeiro os
     * que possuem validade mais próxima.
     *
     * A versão definitiva precisará definir o comportamento dos lotes sem
     * dataValidade para produtos não perecíveis.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT lote
            FROM Lote lote
            WHERE lote.estoqueCentral.id = :estoqueId
              AND lote.ativo = true
              AND lote.quantidadeDisponivel > 0
              AND lote.dataValidade IS NOT NULL
            ORDER BY lote.dataValidade ASC, lote.id ASC
            """)
    List<Lote> buscarDisponiveisPorFefoComBloqueio(
            @Param("estoqueId") Long estoqueId
    );

}
