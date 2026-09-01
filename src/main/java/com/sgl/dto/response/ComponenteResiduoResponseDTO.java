package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.ComponenteResiduo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Componente que participa da composição de um resíduo.")
@Getter
public class ComponenteResiduoResponseDTO {

    private final UUID id;
    private final UUID produtoId;
    private final String produtoNomeCatalogo;
    private final String nomeComponente;
    private final Boolean principal;
    private final String concentracaoOuQuantidade;
    private final String observacao;

    public ComponenteResiduoResponseDTO(ComponenteResiduo entity) {
        this.id = entity.getPublicId();
        this.produtoId = entity.getProduto() != null ? entity.getProduto().getPublicId() : null;
        this.produtoNomeCatalogo = entity.getProduto() != null ? entity.getProduto().getNome() : null;
        this.nomeComponente = entity.getNomeComponente();
        this.principal = entity.getPrincipal();
        this.concentracaoOuQuantidade = entity.getConcentracaoOuQuantidade();
        this.observacao = entity.getObservacao();
    }
}
