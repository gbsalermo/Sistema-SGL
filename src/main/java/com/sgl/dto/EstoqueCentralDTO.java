package com.sgl.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.EstoqueCentral;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueCentralDTO {

    private Long id;

    @NotNull(message = "Id da unidade é obrigatório")
    private Long unidadeId;

    @NotNull(message = "Id do produto é obrigatorio")
    private Long produtoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String unidadeNome;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String unidadeSigla;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String produtoNome;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String produtoUnidadeArmazenamento;

    /**
     * Saldo agregado. Não pode ser informado diretamente pelo cliente;
     * será alterado somente pelas operações físicas de movimentação por lote.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer quantidadeAtual;

    @NotNull(message = "quantidade minima é obrigatoria")
    @Min(value = 0, message = "quantidade mínima não pode ser negativa")
    private Integer quantidadeMinima;

    private Boolean ativo;

    public EstoqueCentralDTO(EstoqueCentral entity) {
        this.id = entity.getId();
        this.unidadeId = entity.getUnidade().getId();
        this.unidadeNome = entity.getUnidade().getNome();
        this.unidadeSigla = entity.getUnidade().getSigla();
        this.produtoId = entity.getProduto().getId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.quantidadeMinima = entity.getQuantidadeMinima();
        this.ativo = entity.getAtivo();
    }
}
