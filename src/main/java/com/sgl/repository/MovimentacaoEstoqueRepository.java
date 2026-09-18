package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.TipoMovimentacao;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Optional<MovimentacaoEstoque> findByPublicId(UUID publicId);
    Optional<MovimentacaoEstoque> findByPublicIdAndEstoqueCentralUnidadePublicId(UUID publicId, UUID unidadePublicId);

    List<MovimentacaoEstoque> findByEstoqueCentralUnidadePublicId(UUID unidadePublicId);

    @Override
    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            ORDER BY m.dataMovimentacao DESC
            """)
    List<MovimentacaoEstoque> findAll();

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.produto.id = :produtoId
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<MovimentacaoEstoque> findByProdutoId(@Param("produtoId") Long produtoId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.laboratorio.id = :laboratorioId
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<MovimentacaoEstoque> findByLaboratorioId(@Param("laboratorioId") Long laboratorioId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.pedido.id = :pedidoId
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<MovimentacaoEstoque> findByPedidoId(@Param("pedidoId") Long pedidoId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.lote.id = :loteId
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            ORDER BY m.dataMovimentacao DESC
            """)
    List<MovimentacaoEstoque> findByLoteIdOrderByDataMovimentacaoDesc(@Param("loteId") Long loteId);

    List<MovimentacaoEstoque> findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
            Long pedidoId,
            TipoMovimentacao tipoMovimentacao
    );

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.tipoMovimentacao = :tipo
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<MovimentacaoEstoque> findByTipoMovimentacao(@Param("tipo") TipoMovimentacao tipo);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.usuario.id = :usuarioId
              AND (:#{@tenantProvider.unidadeId} IS NULL
               OR m.estoqueCentral.unidade.publicId = :#{@tenantProvider.unidadeId})
            """)
    List<MovimentacaoEstoque> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
