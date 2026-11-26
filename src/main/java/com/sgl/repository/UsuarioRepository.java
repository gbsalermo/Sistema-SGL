package com.sgl.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Usuario;
import com.sgl.model.enums.CategoriaCargo;
import com.sgl.model.enums.PapelSistema;
import com.sgl.model.enums.SituacaoUsuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{

    Optional<Usuario> findByPublicId(UUID publicId);

    // The security filter runs outside any persistence context, so the Unidade must come loaded.
    @EntityGraph(attributePaths = "unidade")
    Optional<Usuario> findWithUnidadeByPublicId(UUID publicId);

    // Session DTOs are built outside a transaction (login is deliberately not transactional).
    @EntityGraph(attributePaths = {"unidade", "laboratorios"})
    Optional<Usuario> findWithUnidadeAndLaboratoriosByPublicId(UUID publicId);

    Optional<Usuario> findByPublicIdAndUnidadePublicId(UUID publicId, UUID unidadePublicId);

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByMatricula(String matricula);
    Optional<Usuario> findByCpf(String cpf);
    Optional<Usuario> findByLogin(String login);

    // Login is not transactional and a change of Unidade clears the list, so it must come loaded.
    @EntityGraph(attributePaths = "laboratorios")
    Optional<Usuario> findWithLaboratoriosByMatricula(String matricula);
    @EntityGraph(attributePaths = "laboratorios")
    Optional<Usuario> findWithLaboratoriosByCpf(String cpf);

    // Counts inactive users too: the first-login bootstrap must never run twice.
    @Query("SELECT COUNT(u) > 0 FROM Usuario u JOIN u.papeis p WHERE p = :papel")
    boolean existeComPapel(@Param("papel") PapelSistema papel);

    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.papeis p WHERE p = :papel AND u.ativo = true")
    long contarAtivosComPapel(@Param("papel") PapelSistema papel);

    @Query("""
            SELECT COUNT(u)
            FROM Usuario u JOIN u.papeis p
            WHERE p = :papel AND u.ativo = true AND u.unidade.publicId = :unidadePublicId
            """)
    long contarAtivosComPapelNaUnidade(@Param("papel") PapelSistema papel,
                                       @Param("unidadePublicId") UUID unidadePublicId);

    List<Usuario> findByMatriculaIn(Collection<String> matriculas);
    List<Usuario> findByCpfIn(Collection<String> cpfs);
    List<Usuario> findBySituacaoAndUnidadePublicId(SituacaoUsuario situacao, UUID unidadePublicId);

    List<Usuario> findByUnidadePublicIdAndAtivoTrueAndSituacao(UUID unidadePublicId, SituacaoUsuario situacao);

    // Approved interns have no row in estagiarios until management creates the internship link.
    @Query("""
            SELECT u
            FROM Usuario u
            LEFT JOIN FETCH u.laboratorios
            WHERE u.unidade.publicId = :unidadePublicId
              AND u.categoriaCargo = com.sgl.model.enums.CategoriaCargo.ESTAGIARIO
              AND u.situacao = com.sgl.model.enums.SituacaoUsuario.ATIVO
              AND u.ativo = true
              AND NOT EXISTS (SELECT e FROM Estagiario e WHERE e.id = u.id)
            ORDER BY u.nome
            """)
    List<Usuario> findEstagiariosSemVinculo(@Param("unidadePublicId") UUID unidadePublicId);

    // Dev-only login picker. Not tenant-filtered on purpose: /auth routes run before any tenant is set.
    // Explicit LEFT JOIN so users without a Unidade (e.g. global admins) are not dropped by an implicit inner join.
    // Ids only (no fetch) so pagination happens in SQL; the entities are loaded by findWithUnidadeAndLaboratoriosByIdIn.
    @Query("""
            SELECT u.id
            FROM Usuario u
            LEFT JOIN u.unidade un
            WHERE u.ativo = true
              AND u.login IS NOT NULL
              AND (:papel IS NULL OR :papel MEMBER OF u.papeis)
              AND (:categoria IS NULL OR (u.categoriaCargo = :categoria AND u.papeis IS EMPTY))
              AND (:unidadeId IS NULL OR un.publicId = :unidadeId)
              AND (:busca IS NULL
                   OR LOWER(u.nome) LIKE :busca ESCAPE '\\'
                   OR LOWER(u.login) LIKE :busca ESCAPE '\\')
            ORDER BY u.nome, u.id
            """)
    List<Long> buscarIdsParaLoginDeTeste(@Param("papel") PapelSistema papel,
                                         @Param("categoria") CategoriaCargo categoria,
                                         @Param("unidadeId") UUID unidadeId,
                                         @Param("busca") String busca,
                                         Pageable pageable);

    // Second step of the paged lookup: collections are fetched here, never together with pagination.
    @EntityGraph(attributePaths = {"unidade", "laboratorios"})
    List<Usuario> findWithUnidadeAndLaboratoriosByIdIn(Collection<Long> ids);

    @Query("""
            SELECT p, COUNT(u)
            FROM Usuario u
            JOIN u.papeis p
            LEFT JOIN u.unidade un
            WHERE u.ativo = true
              AND u.login IS NOT NULL
              AND (:unidadeId IS NULL OR un.publicId = :unidadeId)
            GROUP BY p
            """)
    List<Object[]> contarAtivosComLoginPorPapel(@Param("unidadeId") UUID unidadeId);

    // Users with no role, grouped by job category: the former "solicitante" profiles.
    @Query("""
            SELECT u.categoriaCargo, COUNT(u)
            FROM Usuario u
            LEFT JOIN u.unidade un
            WHERE u.ativo = true
              AND u.login IS NOT NULL
              AND u.papeis IS EMPTY
              AND (:unidadeId IS NULL OR un.publicId = :unidadeId)
            GROUP BY u.categoriaCargo
            """)
    List<Object[]> contarAtivosComLoginSemPapelPorCategoria(@Param("unidadeId") UUID unidadeId);

    @Override
    @Query("""
            SELECT usuario
            FROM Usuario usuario
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR usuario.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<Usuario> findAll();

    List<Usuario> findByLaboratoriosId(Long laboratorioId);
    List<Usuario> findByUnidadePublicId(UUID unidadePublicId);

    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
