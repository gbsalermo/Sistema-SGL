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

    List<MovimentacaoEstoque> findByProdutoId(Long produtoId);

    List<MovimentacaoEstoque> findByLaboratorioId(Long laboratorioId);

    List<MovimentacaoEstoque> findByPedidoId(Long pedidoId);

    List<MovimentacaoEstoque> findByPedidoIdAndTipoMovimentacaoOrderByIdAsc(
            Long pedidoId,
            TipoMovimentacao tipoMovimentacao
    );

    List<MovimentacaoEstoque> findByTipoMovimentacao(TipoMovimentacao tipo);

    List<MovimentacaoEstoque> findByUsuarioId(Long usuarioId);
}
