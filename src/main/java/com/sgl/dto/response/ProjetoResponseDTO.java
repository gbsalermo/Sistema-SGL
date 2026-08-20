package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Projeto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "Representação de um projeto retornado pela API.")
@Getter
public class ProjetoResponseDTO {

    @Schema(description = "Identificador público UUID do projeto.", example = "550e8400-e29b-41d4-a716-446655440003")
    private final UUID id;
    @Schema(description = "Identificador público UUID do laboratório vinculado.", example = "550e8400-e29b-41d4-a716-446655440002")
    private final UUID laboratorioId;
    @Schema(description = "Nome do laboratório vinculado.", example = "Laboratório de Química Orgânica")
    private final String laboratorioNome;
    @Schema(description = "Nome do projeto.", example = "Síntese de Novos Compostos")
    private final String nome;
    @Schema(description = "Descrição do projeto.", example = "Desenvolvimento de novos compostos orgânicos para catálise.")
    private final String descricao;
    @Schema(description = "Data de início do projeto.", example = "2026-07-12")
    private final LocalDate dataInicio;
    @Schema(description = "Data de término do projeto, quando definida.", example = "2026-12-20")
    private final LocalDate dataFim;
    @Schema(description = "Nome do responsável pelo projeto.", example = "Maria Oliveira")
    private final String responsavel;
    @Schema(description = "Indica se o projeto está ativo.", example = "true")
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
