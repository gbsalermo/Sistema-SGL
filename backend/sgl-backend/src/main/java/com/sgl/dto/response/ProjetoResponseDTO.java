package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Projeto;

import lombok.Getter;

@Getter
public class ProjetoResponseDTO {

    private final UUID id;
    private final UUID laboratorioId;
    private final String laboratorioNome;
    private final String nome;
    private final String descricao;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final String responsavel;
    private final Boolean ativo;

    public ProjetoResponseDTO(Projeto entity) {
        this.id = entity.getPublicId();
        this.laboratorioId = entity.getLaboratorio() != null ? entity.getLaboratorio().getPublicId() : null;
        this.laboratorioNome = entity.getLaboratorio() != null ? entity.getLaboratorio().getNome() : null;
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.dataInicio = entity.getDataInicio();
        this.dataFim = entity.getDataFim();
        this.responsavel = entity.getResponsavel();
        this.ativo = entity.getAtivo();
    }
}
