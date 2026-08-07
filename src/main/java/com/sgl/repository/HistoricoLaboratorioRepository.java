package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.HistoricoLaboratorio;

@Repository
public interface HistoricoLaboratorioRepository extends JpaRepository<HistoricoLaboratorio, Long> {

    List<HistoricoLaboratorio> findByLaboratorioId(Long laboratorioId);

    List<HistoricoLaboratorio> findByProdutoId(Long produtoId);

    List<HistoricoLaboratorio> findByPedidoId(Long pedidoId);

    /**
     * Retorna os materiais efetivamente recebidos por um laboratório dentro de
     * um intervalo de datas.
     */
    @Query("""
            SELECT historico
            FROM HistoricoLaboratorio historico
            WHERE historico.laboratorio.id = :laboratorioId
              AND historico.dataRecebimento BETWEEN :dataInicio AND :dataFim
            ORDER BY historico.dataRecebimento ASC, historico.id ASC
            """)
    List<HistoricoLaboratorio> findByLaboratorioIdAndPeriodo(
            @Param("laboratorioId") Long laboratorioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    /**
     * Retorna somente os materiais recebidos em pedidos vinculados ao projeto
     * informado, dentro do laboratório e período especificados.
     */
    @Query("""
            SELECT historico
            FROM HistoricoLaboratorio historico
            WHERE historico.laboratorio.id = :laboratorioId
              AND historico.pedido.projeto.id = :projetoId
              AND historico.dataRecebimento BETWEEN :dataInicio AND :dataFim
            ORDER BY historico.dataRecebimento ASC, historico.id ASC
            """)
    List<HistoricoLaboratorio> findByLaboratorioProjetoEPeriodo(
            @Param("laboratorioId") Long laboratorioId,
            @Param("projetoId") Long projetoId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}
