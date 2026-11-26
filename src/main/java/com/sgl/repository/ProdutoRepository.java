package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;

import jakarta.persistence.LockModeType;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByPublicId(UUID publicId);

    @Override
    @Query("""
            SELECT DISTINCT produto
            FROM Produto produto
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR EXISTS (
                    SELECT estoque.id
                    FROM EstoqueCentral estoque
                    WHERE estoque.produto = produto
                      AND estoque.unidade.publicId = :#{@tenantProvider.unidadeId}
               ))
            """)
    List<Produto> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.id = :id")
    Optional<Produto> buscarPorIdComBloqueio(@Param("id") Long id);

    List<Produto> findByRisco(NivelRisco risco);

    List<Produto> findByPerecivelTrue();

    List<Produto> findByNomeContainingIgnoreCase(String nome);

    @Query("""
            SELECT DISTINCT estoque.produto
            FROM EstoqueCentral estoque
            WHERE estoque.unidade.publicId = :unidadePublicId
            """)
    List<Produto> findDisponiveisNaUnidade(@Param("unidadePublicId") UUID unidadePublicId);

    @Query("""
            SELECT CASE WHEN COUNT(estoque) > 0 THEN true ELSE false END
            FROM EstoqueCentral estoque
            WHERE estoque.unidade.publicId = :unidadePublicId
              AND estoque.produto.publicId = :produtoPublicId
            """)
    boolean pertenceAUnidade(
            @Param("produtoPublicId") UUID produtoPublicId,
            @Param("unidadePublicId") UUID unidadePublicId
    );

    /**
     * Catálogo administrativo da Unidade: inclui produtos ainda sem estoque,
     * mas não expõe produtos cadastrados exclusivamente em outra Unidade.
     */
    @Query("""
            SELECT DISTINCT p FROM Produto p
            LEFT JOIN p.unidadeCadastro uc
            WHERE uc.publicId = :unidadeId
               OR EXISTS (
                   SELECT ec.id FROM EstoqueCentral ec
                   WHERE ec.produto = p
                     AND ec.unidade.publicId = :unidadeId
               )
            ORDER BY p.nome
            """)
    List<Produto> findCatalogoAdministrativoDaUnidade(
            @Param("unidadeId") UUID unidadeId);

    boolean existsByCodigoReferencia(String codigoReferencia);

    boolean existsByCodigoReferenciaAndIdNot(
            String codigoReferencia,
            Long id
    );
}
