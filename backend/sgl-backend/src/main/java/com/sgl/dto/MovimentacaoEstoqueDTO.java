package com.sgl.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimentacaoEstoqueDTO {

    private UUID id;

    @NotNull(message = "Id do produto é obrigatorio")
    private UUID produtoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String produtoNome;

    private UUID laboratorioId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String laboratorioNome;

    @NotNull(message = "Id do usuario é obrigatorio")
    private UUID usuarioId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String usuarioNome;

    @NotNull(message = "Id do estoque é obrigatório")
    private UUID estoqueCentralId;

    private UUID pedidoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID loteId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String numeroLote;

    @NotNull(message = "Tipo da movimentação é obrigatório")
    private TipoMovimentacao tipoMovimentacao;

    @NotNull(message = "Origem é obrigatório")
    private OrigemMovimentacao origem;

    @NotNull(message = "Quantidade movimentada é obrigatória")
    private Integer quantidadeMovimentada;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer quantidadeAnterior;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer quantidadeAtual;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime dataMovimentacao;

    private String observacao;

    public MovimentacaoEstoqueDTO(MovimentacaoEstoque entity) {
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
        this.quantidadeMovimentada = entity.getQuantidadeMovimentada();
        this.quantidadeAnterior = entity.getQuantidadeAnterior();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.dataMovimentacao = entity.getDataMovimentacao();
        this.observacao = entity.getObservacao();
        this.origem = entity.getOrigem();
    }
}
