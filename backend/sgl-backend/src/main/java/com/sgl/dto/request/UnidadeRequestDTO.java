package com.sgl.dto.request;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Dados necessários para cadastrar ou atualizar uma unidade institucional.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Nome completo da unidade.", example = "Instituto de Química", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @Schema(description = "Sigla da unidade.", example = "IQ", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Sigla é obrigatória")
    private String sigla;
}
