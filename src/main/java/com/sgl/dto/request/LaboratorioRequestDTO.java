package com.sgl.dto.request;

import java.io.Serializable;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dados necessários para cadastrar ou atualizar um laboratório.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LaboratorioRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador público UUID da unidade à qual o laboratório pertence.", example = "550e8400-e29b-41d4-a716-446655440002", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "unidadeId é obrigatório")
    private UUID unidadeId;

    @Schema(description = "Nome do laboratório.", example = "Laboratório de Química Orgânica", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "nome é obrigatório")
    private String nome;

    @Schema(description = "Descrição do laboratório.", example = "Laboratório destinado a atividades de síntese e análise de compostos orgânicos.")
    private String descricao;

    @Schema(description = "Identificador público UUID do usuário responsável pelo laboratório, quando definido.", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID responsavelId;

    @Schema(description = "Indica se o laboratório está ativo.", example = "true")
    private Boolean ativo;
}
