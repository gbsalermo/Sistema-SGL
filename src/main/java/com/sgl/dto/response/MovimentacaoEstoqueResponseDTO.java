package com.sgl.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueResponseDTO {

    private UUID id;
    private UUID produtoId;
    private String produtoNome;
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID usuarioId;
    private String usuarioNome;
    private UUID estoqueCentralId;
    private UUID pedidoId;
    private UUID loteId;
    private String numeroLote;
    private TipoMovimentacao tipoMovimentacao;
    private OrigemMovimentacao origem;
    private Integer quantidadeMovimentada;
    private Integer quantidadeAnterior;
    private Integer quantidadeAtual;
    private LocalDateTime dataMovimentacao;
    private String observacao;

    public MovimentacaoEstoqueResponseDTO(MovimentacaoEstoque entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.laboratorioId = entity.getLaboratorio() != null
                ? entity.getLaboratorio().getPublicId()
                : null;
        this.laboratorioNome = entity.getLaboratorio() != null
                ? entity.getLaboratorio().getNome()
                : null;
        this.usuarioId = entity.getUsuario().getPublicId();
        this.usuarioNome = entity.getUsuario().getNome();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.pedidoId = entity.getPedido() != null
                ? entity.getPedido().getPublicId()
                : null;
        this.loteId = entity.getLote() != null ? entity.getLote().getPublicId() : null;
        this.numeroLote = entity.getLote() != null ? entity.getLote().getNumeroLote() : null;
        this.tipoMovimentacao = entity.getTipoMovimentacao();
        this.origem = entity.getOrigem();
        this.quantidadeMovimentada = entity.getQuantidadeMovimentada();
        this.quantidadeAnterior = entity.getQuantidadeAnterior();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.dataMovimentacao = entity.getDataMovimentacao();
        this.observacao = entity.getObservacao();
    }
}
