package com.sgl.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Solucao;
import com.sgl.model.enums.UnidadeMedida;

import lombok.Getter;

@Getter
public class SolucaoResponseDTO {

    private final UUID id;

    private final UUID unidadeId;
    private final String unidadeNome;
    private final String unidadeSigla;

    private final String nome;
    private final String descricao;

    private final BigDecimal rendimentoQuantidade;
    private final UnidadeMedida rendimentoUnidade;

    private final String concentracao;
    private final String instrucoesPreparo;
    private final Boolean ativo;

    private final List<SolucaoComponenteResponseDTO> componentes;

    public SolucaoResponseDTO(Solucao entity) {
        this.id = entity.getPublicId();

        this.unidadeId = entity.getUnidade().getPublicId();
        this.unidadeNome = entity.getUnidade().getNome();
        this.unidadeSigla = entity.getUnidade().getSigla();

        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();

        this.rendimentoQuantidade = entity.getRendimentoQuantidade();
        this.rendimentoUnidade = entity.getRendimentoUnidade();

        this.concentracao = entity.getConcentracao();
        this.instrucoesPreparo = entity.getInstrucoesPreparo();
        this.ativo = entity.getAtivo();

        this.componentes = entity.getComponentes().stream()
                .map(SolucaoComponenteResponseDTO::new)
                .toList();
    }
}
