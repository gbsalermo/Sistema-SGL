package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sgl.model.Estagiario;

@Repository
public interface EstagiarioRepository extends JpaRepository<Estagiario, Long> {

    @Override
    @Query("""
            SELECT estagiario
            FROM Estagiario estagiario
            WHERE (:#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)} IS NULL
               OR estagiario.unidade.publicId = :#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)})
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
}
