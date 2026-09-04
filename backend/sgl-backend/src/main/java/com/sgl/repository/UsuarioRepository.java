package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sgl.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Optional<Usuario> findByPublicId(UUID publicId);
    Optional<Usuario> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    Optional<Usuario> findByEmail(String email);

    @Override
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE (:#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)} IS NULL
               OR usuario.unidade.publicId = :#{T(com.sgl.tenant.TenantContext).unidadeAtual().orElse(null)})
            """)
    List<Usuario> findAll();

    List<Usuario> findByLaboratorioId(Long laboratorioId);
    List<Usuario> findByUnidadePublicId(UUID unidadePublicId);

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
