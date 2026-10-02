package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import com.sgl.model.Estagiario;

@Repository
public interface EstagiarioRepository extends JpaRepository<Estagiario, Long> {

    @Override
    @Query("""
            SELECT estagiario
            FROM Estagiario estagiario
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR estagiario.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<Estagiario> findAll();

    List<Estagiario> findByLaboratorioId(Long laboratorioId);
    List<Estagiario> findByUnidadePublicId(UUID unidadePublicId);
    List<Estagiario> findByUnidadePublicIdAndAtivoTrue(UUID unidadePublicId);

    List<Estagiario> findByAtivoTrue();

    boolean existsByIdAndAtivoTrue(Long usuarioId);

    Optional<Estagiario> findById(Long id);
    Optional<Estagiario> findByPublicId(UUID publicId);
    Optional<Estagiario> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);
    
    @Query("""
            SELECT DISTINCT e
            FROM Estagiario e
            WHERE e.unidade.publicId = :unidadeId
              AND e.ativo = true
              AND EXISTS (
                  SELECT v.id
                  FROM VinculoEstagio v
                  WHERE v.estagiario = e
                    AND v.situacao <> com.sgl.model.enums.SituacaoEstagio.FINALIZADO
                    AND EXISTS (
                        SELECT p.id
                        FROM VinculoEstagioAtividade p
                        WHERE p.vinculoEstagio = v
                          AND p.dataFimParticipacao IS NULL
                    )
              )
            """)
    List<Estagiario> findEstagiariosComVinculoAtivo(
            @Param("unidadeId") UUID unidadeId
    );
    
    
}
