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

    boolean existsByCodigoReferencia(String codigoReferencia);

    boolean existsByCodigoReferenciaAndIdNot(
            String codigoReferencia,
            Long id
    );
}
