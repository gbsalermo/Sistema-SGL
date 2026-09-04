package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sgl.model.Laboratorio;

@Repository
public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {

    Optional<Laboratorio> findByPublicId(UUID publicId);
    Optional<Laboratorio> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    @Override
    @Query("""
            SELECT laboratorio
            FROM Laboratorio laboratorio
            WHERE (:#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)} IS NULL
               OR laboratorio.unidade.publicId = :#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)})
            """)
    List<Laboratorio> findAll();

    List<Laboratorio> findByUnidadeId(Long unidadeId);
    List<Laboratorio> findByUnidadePublicId(UUID unidadePublicId);
}
