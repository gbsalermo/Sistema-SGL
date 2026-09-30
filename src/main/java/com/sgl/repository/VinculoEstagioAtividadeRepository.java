package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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

    boolean existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
            Long vinculoEstagioId,
            Long atividadeId
        );
}