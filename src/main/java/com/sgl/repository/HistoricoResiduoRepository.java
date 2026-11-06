package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.HistoricoResiduo;

@Repository
public interface HistoricoResiduoRepository extends JpaRepository<HistoricoResiduo, Long> {

    List<HistoricoResiduo> findByResiduoIdOrderByDataHoraAsc(Long residuoId);

    // O evento de liberação guarda o usuário que efetivamente a realizou.
    // A busca por ID é segura neste fluxo porque o serviço já exige tenant ativo
    // e carrega o resíduo com filtro de Unidade antes de consultar seu histórico.
    @EntityGraph(attributePaths = "usuario")
    Optional<HistoricoResiduo> findFirstByResiduoIdAndAcaoOrderByDataHoraDesc(Long residuoId, String acao);

    @Query("""
            SELECT historico
            FROM HistoricoResiduo historico
            WHERE historico.residuo.gerador.publicId = :geradorPublicId
              AND historico.residuo.laboratorio.unidade.publicId = :unidadePublicId
            ORDER BY historico.dataHora DESC
            """)
    List<HistoricoResiduo> findByResiduoGeradorPublicIdAndResiduoLaboratorioUnidadePublicIdOrderByDataHoraDesc(
            @Param("geradorPublicId") UUID geradorPublicId,
            @Param("unidadePublicId") UUID unidadePublicId
    );

    @Query("""
            SELECT historico
            FROM HistoricoResiduo historico
            WHERE historico.residuo.laboratorio.unidade.publicId = :unidadePublicId
            ORDER BY historico.dataHora DESC
            """)
    List<HistoricoResiduo> findByResiduoLaboratorioUnidadePublicIdOrderByDataHoraDesc(
            @Param("unidadePublicId") UUID unidadePublicId
    );
}
