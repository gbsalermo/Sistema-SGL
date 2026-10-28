package com.sgl.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sgl.model.Usuario;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Optional<Usuario> findByPublicId(UUID publicId);

    // The security filter runs outside any persistence context, so the Unidade must come loaded.
    @EntityGraph(attributePaths = "unidade")
    Optional<Usuario> findWithUnidadeByPublicId(UUID publicId);

    // Session DTOs are built outside a transaction (login is deliberately not transactional).
    @EntityGraph(attributePaths = {"unidade", "laboratorio"})
    Optional<Usuario> findWithUnidadeAndLaboratorioByPublicId(UUID publicId);

    Optional<Usuario> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByMatricula(String matricula);
    Optional<Usuario> findByCpf(String cpf);
    Optional<Usuario> findByLogin(String login);

    boolean existsByPerfilAndAtivoTrue(Perfil perfil);
    boolean existsByPerfil(Perfil perfil);
    long countByPerfilAndAtivoTrue(Perfil perfil);

    List<Usuario> findByMatriculaIn(Collection<String> matriculas);
    List<Usuario> findByCpfIn(Collection<String> cpfs);
    List<Usuario> findBySituacaoAndUnidadePublicId(SituacaoUsuario situacao, UUID unidadePublicId);

    @Override
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR usuario.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<Usuario> findAll();

    List<Usuario> findByLaboratorioId(Long laboratorioId);
    List<Usuario> findByUnidadePublicId(UUID unidadePublicId);

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
