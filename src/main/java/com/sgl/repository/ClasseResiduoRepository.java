package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.ClasseResiduo;

@Repository
public interface ClasseResiduoRepository
        extends JpaRepository<ClasseResiduo, Long> {

    Optional<ClasseResiduo> findByPublicId(UUID publicId);

    Optional<ClasseResiduo>
            findByPublicIdAndUnidadePublicId(
                    UUID publicId,
                    UUID unidadePublicId
            );

    List<ClasseResiduo>
            findByUnidadePublicIdOrderByCodigoAsc(
                    UUID unidadePublicId
            );

    List<ClasseResiduo>
            findByUnidadePublicIdAndAtivoTrueOrderByCodigoAsc(
                    UUID unidadePublicId
            );

    List<ClasseResiduo>
            findByAtivoTrueOrderByCodigoAsc();

    List<ClasseResiduo>
            findByPublicIdInAndUnidadePublicId(
                    Set<UUID> ids,
                    UUID unidadePublicId
            );

    boolean existsByUnidadeIdAndCodigoIgnoreCase(
            Long unidadeId,
            String codigo
    );

    boolean existsByUnidadeIdAndCodigoIgnoreCaseAndIdNot(
            Long unidadeId,
            String codigo,
            Long id
    );
}