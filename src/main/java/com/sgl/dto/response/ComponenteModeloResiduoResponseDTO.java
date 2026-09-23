package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ComponenteModeloResiduo;

import lombok.Getter;

@Getter
public class ComponenteModeloResiduoResponseDTO {

    private final UUID id;

    private final UUID produtoId;
    private final String produtoNome;

    private final String nomeComponente;
    private final Boolean principal;
    private final String concentracaoOuQuantidade;
    private final String observacao;

    public ComponenteModeloResiduoResponseDTO(
            ComponenteModeloResiduo entity) {

        this.id = entity.getPublicId();

        this.produtoId = entity.getProduto() != null
                ? entity.getProduto().getPublicId()
                : null;

        this.produtoNome = entity.getProduto() != null
                ? entity.getProduto().getNome()
                : null;

        this.nomeComponente = entity.getNomeComponente();
        this.principal = entity.getPrincipal();
        this.concentracaoOuQuantidade =
                entity.getConcentracaoOuQuantidade();
        this.observacao = entity.getObservacao();
    }
}