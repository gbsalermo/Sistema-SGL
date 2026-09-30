package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.VinculoEstagio;
import com.sgl.model.enums.SituacaoEstagio;

@Repository
public interface VinculoEstagioRepository
        extends JpaRepository<VinculoEstagio, Long> {

    Optional<VinculoEstagio>
        findByPublicIdAndEstagiarioUnidadePublicId(
            UUID publicId,
            UUID unidadePublicId
        );

    List<VinculoEstagio>
        findByEstagiarioPublicIdAndEstagiarioUnidadePublicIdOrderByDataInicioDesc(
            UUID estagiarioPublicId,
            UUID unidadePublicId
        );
    boolean existsByEstagiarioIdAndSituacaoNot(
            Long estagiarioId,
            SituacaoEstagio situacao
    );
}