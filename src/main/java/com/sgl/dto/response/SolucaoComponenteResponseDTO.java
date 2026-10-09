package com.sgl.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.sgl.model.SolucaoComponente;
import com.sgl.model.enums.UnidadeMedida;
import com.sgl.model.medida.ConversorUnidadeMedida;

import lombok.Getter;

@Getter
public class SolucaoComponenteResponseDTO {

    private final UUID id;
    private final Integer ordem;

    private final UUID produtoId;
    private final String produtoNome;
    private final String produtoCodigoReferencia;

    private final BigDecimal quantidade;
    private final UnidadeMedida unidadeMedida;

    private final BigDecimal quantidadeCanonica;
    private final UnidadeMedida unidadeCanonica;

    public SolucaoComponenteResponseDTO(SolucaoComponente entity) {
        this.id = entity.getPublicId();
        this.ordem = entity.getOrdem();

        this.produtoId = entity.getProduto().getPublicId();
        this.produtoNome = entity.getProduto().getNome();
        this.produtoCodigoReferencia = entity.getProduto().getCodigoReferencia();

        this.quantidade = entity.getQuantidade();
        this.unidadeMedida = entity.getUnidadeMedida();

        this.unidadeCanonica = entity.getProduto().getUnidadeMedida();
        this.quantidadeCanonica = ConversorUnidadeMedida.converterParaCanonica(
                entity.getQuantidade(),
                entity.getUnidadeMedida(),
                entity.getProduto().getUnidadeMedida()
        );
    }
}
