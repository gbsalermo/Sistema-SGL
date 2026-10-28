package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Laboratorio;
import com.sgl.model.enums.SituacaoUsuario;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {

    Optional<Laboratorio> findByPublicId(UUID publicId);
    Optional<Laboratorio> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    @Override
    @Query("""
            SELECT laboratorio
            FROM Laboratorio laboratorio
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR laboratorio.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<Laboratorio> findAll();

    List<Laboratorio> findByUnidadeId(Long unidadeId);
    List<Laboratorio> findByUnidadePublicId(UUID unidadePublicId);

    /**
     * Laboratórios ativos da unidade cujo responsável não pode mais exercer o cargo:
     * está inativo, não está ATIVO no SGL ou pertence a outra unidade (mudou de lotação).
     */
    @Query("""
            SELECT laboratorio
            FROM Laboratorio laboratorio
            JOIN FETCH laboratorio.responsavel responsavel
            WHERE laboratorio.unidade.publicId = :unidadeId
              AND laboratorio.ativo = true
              AND (responsavel.ativo = false
                   OR responsavel.situacao <> :liberado
                   OR responsavel.unidade IS NULL
                   OR responsavel.unidade <> laboratorio.unidade)
            ORDER BY laboratorio.nome
            """)
    List<Laboratorio> findComResponsavelSemCondicoes(@Param("unidadeId") UUID unidadeId,
            @Param("liberado") SituacaoUsuario liberado);
}
