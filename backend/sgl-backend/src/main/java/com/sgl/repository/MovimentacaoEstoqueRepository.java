package com.sgl.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.TipoMovimentacao;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Optional<MovimentacaoEstoque> findByPublicId(UUID publicId);
    Optional<MovimentacaoEstoque> findByPublicIdAndEstoqueCentralUnidadePublicId(UUID publicId, UUID unidadePublicId);

    List<MovimentacaoEstoque> findByEstoqueCentralUnidadePublicId(UUID unidadePublicId);

    List<MovimentacaoEstoque> findByProdutoId(Long produtoId);
    List<MovimentacaoEstoque> findByProdutoIdAndEstoqueCentralUnidadePublicId(Long produtoId, UUID unidadePublicId);

    List<MovimentacaoEstoque> findByLaboratorioId(Long laboratorioId);
    List<MovimentacaoEstoque> findByLaboratorioIdAndEstoqueCentralUnidadePublicId(Long laboratorioId, UUID unidadePublicId);

    List<MovimentacaoEstoque> findByPedidoId(Long pedidoId);
    List<MovimentacaoEstoque> findByPedidoIdAndEstoqueCentralUnidadePublicId(Long pedidoId, UUID unidadePublicId);

    List<MovimentacaoEstoque> findByLoteIdOrderByDataMovimentacaoDesc(Long loteId);

    List<MovimentacaoEstoque> findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
            Long pedidoId,
            TipoMovimentacao tipoMovimentacao
    );

    List<MovimentacaoEstoque> findByTipoMovimentacao(TipoMovimentacao tipo);
    List<MovimentacaoEstoque> findByEstoqueCentralUnidadePublicIdAndTipoMovimentacao(UUID unidadePublicId, TipoMovimentacao tipo);

    List<MovimentacaoEstoque> findByUsuarioId(Long usuarioId);
    List<MovimentacaoEstoque> findByUsuarioIdAndEstoqueCentralUnidadePublicId(Long usuarioId, UUID unidadePublicId);
}
