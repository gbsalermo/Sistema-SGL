package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.VinculoEstagioAtividade;

@Repository
public interface VinculoEstagioAtividadeRepository
        extends JpaRepository<VinculoEstagioAtividade, Long> {

    Optional<VinculoEstagioAtividade>
        findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
            UUID publicId,
            UUID unidadePublicId
        );

    List<VinculoEstagioAtividade>
        findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
            UUID vinculoPublicId,
            UUID unidadePublicId
        );

    List<VinculoEstagioAtividade>
        findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdAndDataFimParticipacaoIsNull(
            UUID vinculoPublicId,
            UUID unidadePublicId
        );

    /**
     * A consulta de contexto exige filtros na origem (Estagiario) E no destino
     * (Projeto/Laboratorio); nao basta confiar no header ou filtragem do cliente.
     * EntityGraph evita N+1 ao percorrer a hierarquia de participacoes.
     */
    @EntityGraph(attributePaths = {
            "vinculoEstagio", "vinculoEstagio.estagiario",
            "vinculoEstagio.estagiario.unidade", "atividade", "atividade.sci",
            "atividade.sci.projeto", "atividade.sci.projeto.laboratorio",
            "atividade.sci.projeto.laboratorio.unidade"
    })
    @Query("""
            SELECT p FROM VinculoEstagioAtividade p
            WHERE p.vinculoEstagio.estagiario.publicId = :usuarioId
              AND p.vinculoEstagio.estagiario.unidade.publicId = :unidadeId
              AND p.atividade.sci.projeto.laboratorio.unidade.publicId = :unidadeId
            ORDER BY p.dataInicioParticipacao DESC, p.id DESC
            """)
    List<VinculoEstagioAtividade> listarContextosDoUsuario(
            @Param("usuarioId") UUID usuarioId,
            @Param("unidadeId") UUID unidadeId);

    @EntityGraph(attributePaths = {
            "vinculoEstagio", "vinculoEstagio.estagiario",
            "vinculoEstagio.estagiario.unidade", "atividade", "atividade.sci",
            "atividade.sci.projeto", "atividade.sci.projeto.laboratorio",
            "atividade.sci.projeto.laboratorio.unidade"
    })
    @Query("""
            SELECT p FROM VinculoEstagioAtividade p
            WHERE p.atividade.publicId = :atividadeId
              AND p.vinculoEstagio.estagiario.unidade.publicId = :unidadeId
              AND p.atividade.sci.projeto.laboratorio.unidade.publicId = :unidadeId
            ORDER BY p.dataInicioParticipacao DESC, p.id DESC
            """)
    List<VinculoEstagioAtividade> listarParticipacoesDaAtividade(
            @Param("atividadeId") UUID atividadeId,
            @Param("unidadeId") UUID unidadeId);

    /** Consulta em lote para o hub de Projeto (nao consultar uma Atividade por vez). */
    @EntityGraph(attributePaths = {
            "vinculoEstagio", "vinculoEstagio.estagiario",
            "vinculoEstagio.estagiario.unidade", "atividade", "atividade.sci",
            "atividade.sci.projeto", "atividade.sci.projeto.laboratorio",
            "atividade.sci.projeto.laboratorio.unidade"
    })
    @Query("""
            SELECT p FROM VinculoEstagioAtividade p
            WHERE p.atividade.sci.projeto.publicId = :projetoId
              AND p.vinculoEstagio.estagiario.unidade.publicId = :unidadeId
              AND p.atividade.sci.projeto.laboratorio.unidade.publicId = :unidadeId
            ORDER BY p.atividade.id ASC, p.dataInicioParticipacao DESC, p.id DESC
            """)
    List<VinculoEstagioAtividade> listarParticipacoesDoProjeto(
            @Param("projetoId") UUID projetoId,
            @Param("unidadeId") UUID unidadeId);

    boolean existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
            Long vinculoEstagioId,
            Long atividadeId
        );
    long countByVinculoEstagioIdAndDataFimParticipacaoIsNull(
            Long vinculoEstagioId
    );
}