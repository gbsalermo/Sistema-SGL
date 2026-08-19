package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.EstoqueCentral;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstoqueCentralResponseDTO {

    private UUID id;
    private UUID unidadeId;
    private String unidadeNome;
    private String unidadeSigla;
    private UUID produtoId;
    private String produtoNome;
    private String produtoUnidadeArmazenamento;
    private Integer quantidadeAtual;
    private Integer quantidadeMinima;
    private Boolean ativo;

    public EstoqueCentralResponseDTO(EstoqueCentral entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade().getPublicId();
        this.unidadeNome = entity.getUnidade().getNome();
        this.unidadeSigla = entity.getUnidade().getSigla();
        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoUnidadeArmazenamento = entity.getProduto().getUnidadeArmazenamento();
        this.quantidadeAtual = entity.getQuantidadeAtual();
        this.quantidadeMinima = entity.getQuantidadeMinima();
        this.ativo = entity.getAtivo();
    }
}
