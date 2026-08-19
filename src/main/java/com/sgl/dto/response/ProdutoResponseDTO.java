package com.sgl.dto.response;

import java.util.UUID;

import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.TipoPerecivel;
import com.sgl.model.enums.TipoRisco;
import com.sgl.model.enums.UnidadeMedida;

import lombok.Getter;

@Getter
public class ProdutoResponseDTO {

    private final UUID id;
    private final String nome;
    private final String descricao;
    private final String codigoReferencia;
    private final UnidadeMedida unidadeMedida;
    private final String localizacaoFisica;
    private final NivelRisco risco;
    private final TipoRisco tipoRisco;
    private final String descricaoRisco;
    private final Boolean perecivel;
    private final TipoPerecivel tipoPerecivel;
    private final String condicoesArmazenamento;
    private final String unidadeArmazenamento;
    private final Boolean ativo;

    public ProdutoResponseDTO(Produto entity) {
        this.id = entity.getPublicId();
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.codigoReferencia = entity.getCodigoReferencia();
        this.unidadeMedida = entity.getUnidadeMedida();
        this.localizacaoFisica = entity.getLocalizacaoFisica();
        this.risco = entity.getRisco();
        this.tipoRisco = entity.getTipoRisco();
        this.descricaoRisco = entity.getDescricaoRisco();
        this.perecivel = entity.getPerecivel();
        this.tipoPerecivel = entity.getTipoPerecivel();
        this.condicoesArmazenamento = entity.getCondicoesArmazenamento();
        this.unidadeArmazenamento = entity.getUnidadeArmazenamento();
        this.ativo = entity.getAtivo();
    }
}
