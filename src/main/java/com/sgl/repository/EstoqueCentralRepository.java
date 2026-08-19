package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.EstoqueCentral;

import jakarta.persistence.LockModeType;

@Repository
public interface EstoqueCentralRepository extends JpaRepository<EstoqueCentral, Long> {

    Optional<EstoqueCentral> findByPublicId(UUID publicId);

    Optional<EstoqueCentral> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    boolean existsByUnidadeIdAndProdutoId(
            Long unidadeId,
            Long produtoId
    );

    // Use quando o saldo do estoque for alterado dentro da transação.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT estoque
            FROM EstoqueCentral estoque
            WHERE estoque.id = :id
            """)
    Optional<EstoqueCentral> buscarPorIdComBloqueio(
            @Param("id") Long id
    );

    // Mantém o registro de estoque reservado pela combinação de unidade e produto.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT estoque
            FROM EstoqueCentral estoque
            WHERE estoque.unidade.id = :unidadeId
              AND estoque.produto.id = :produtoId
            """)
    Optional<EstoqueCentral> buscarPorUnidadeEProdutoComBloqueio(
            @Param("unidadeId") Long unidadeId,
            @Param("produtoId") Long produtoId
    );

    List<EstoqueCentral> findByUnidadeId(Long unidadeId);

    List<EstoqueCentral> findByUnidadeIdAndAtivoTrue(Long unidadeId);

    List<EstoqueCentral> findByAtivoTrue();
}
