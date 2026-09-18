package com.sgl.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.HistoricoLaboratorio;

@Repository
public interface HistoricoLaboratorioRepository extends JpaRepository<HistoricoLaboratorio, Long> {

    Optional<HistoricoLaboratorio> findByPublicId(UUID publicId);

    // Correção de segurança: antes o service só tinha o "findByPublicId"
    // acima (sem filtro de unidade) para buscar um registro específico, e
    // "findAll"/"findByProdutoId"/"findByPedidoId" abaixo também não tinham
    // filtro nenhum de unidade. Como HistoricoLaboratorio guarda o material
    // que cada laboratório efetivamente recebeu, isso deixava o histórico
    // de consumo de QUALQUER laboratório de QUALQUER unidade visível para
    // todo mundo. Os métodos abaixo, com "UnidadePublicId" no nome, passam
    // a ser os usados pelo service para aplicar o filtro por unidade.
    Optional<HistoricoLaboratorio> findByPublicIdAndLaboratorioUnidadePublicId(UUID publicId, UUID unidadePublicId);

    List<HistoricoLaboratorio> findByLaboratorioUnidadePublicId(UUID unidadePublicId);

    List<HistoricoLaboratorio> findByLaboratorioId(Long laboratorioId);

    List<HistoricoLaboratorio> findByProdutoIdAndLaboratorioUnidadePublicId(Long produtoId, UUID unidadePublicId);

    List<HistoricoLaboratorio> findByPedidoIdAndLaboratorioUnidadePublicId(Long pedidoId, UUID unidadePublicId);

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

    @Query("""
            SELECT historico
            FROM HistoricoLaboratorio historico
            WHERE historico.laboratorio.id = :laboratorioId
              AND historico.produto.id = :produtoId
              AND historico.dataRecebimento BETWEEN :dataInicio AND :dataFim
              AND historico.ativo = true
            ORDER BY historico.dataRecebimento ASC, historico.id ASC
            """)
    List<HistoricoLaboratorio> findByLaboratorioProdutoEPeriodo(
            @Param("laboratorioId") Long laboratorioId,
            @Param("produtoId") Long produtoId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

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
