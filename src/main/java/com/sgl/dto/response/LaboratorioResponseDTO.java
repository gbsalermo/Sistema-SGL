package com.sgl.dto.response;

import java.io.Serializable;
import java.util.UUID;

import com.sgl.model.Laboratorio;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Representação de um laboratório retornado pela API.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador público UUID do laboratório.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID id;
    @Schema(description = "Identificador público UUID da unidade vinculada.", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID unidadeId;
    @Schema(description = "Nome do laboratório.", example = "Laboratório de Química Orgânica")
    private String nome;
    @Schema(description = "Descrição do laboratório.", example = "Laboratório destinado a atividades de síntese e análise de compostos orgânicos.")
    private String descricao;
    @Schema(description = "Identificador público UUID do responsável, quando definido.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID responsavelId;
    @Schema(description = "Nome do responsável pelo laboratório, quando definido.", example = "Maria Oliveira")
    private String responsavelNome;
    @Schema(description = "Indica se o laboratório está ativo.", example = "true")
    private Boolean ativo;

    public LaboratorioResponseDTO(Laboratorio entity) {
        this.id = entity.getPublicId();
        this.unidadeId = entity.getUnidade() != null ? entity.getUnidade().getPublicId() : null;
        this.nome = entity.getNome();
        this.descricao = entity.getDescricao();
        this.responsavelId = entity.getResponsavel() != null ? entity.getResponsavel().getPublicId() : null;
        this.responsavelNome = entity.getResponsavel() != null ? entity.getResponsavel().getNome() : null;
        this.ativo = entity.getAtivo();
    }
}
