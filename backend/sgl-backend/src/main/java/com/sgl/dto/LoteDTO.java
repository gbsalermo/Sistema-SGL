package com.sgl.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sgl.model.Lote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoteDTO {

    private UUID id;

    private UUID estoqueCentralId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID produtoId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String produtoNome;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID unidadeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String unidadeNome;

    private String numeroLote;

    private Integer quantidadeInicial;

    private Integer quantidadeDisponivel;

    private LocalDate dataEntrada;

    private LocalDate dataValidade;

    private Boolean ativo;

    public LoteDTO(Lote entity) {
        this.id = entity.getPublicId();
        this.estoqueCentralId = entity.getEstoqueCentral().getPublicId();
        this.produtoId = entity.getEstoqueCentral().getProduto().getPublicId();
        this.produtoNome = entity.getEstoqueCentral().getProduto().getNome();
        this.unidadeId = entity.getEstoqueCentral().getUnidade().getPublicId();
        this.unidadeNome = entity.getEstoqueCentral().getUnidade().getNome();
        this.numeroLote = entity.getNumeroLote();
        this.quantidadeInicial = entity.getQuantidadeInicial();
        this.quantidadeDisponivel = entity.getQuantidadeDisponivel();
        this.dataEntrada = entity.getDataEntrada();
        this.dataValidade = entity.getDataValidade();
        this.ativo = entity.getAtivo();
    }
}
