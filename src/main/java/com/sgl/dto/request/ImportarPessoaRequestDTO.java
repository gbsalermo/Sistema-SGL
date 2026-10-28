package com.sgl.dto.request;

import java.util.UUID;

import com.sgl.corporate.TipoPessoaCorporativa;
import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Pessoa corporativa a importar. O CPF nunca trafega: o servidor o busca de novo pela referência.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportarPessoaRequestDTO {

    @Schema(description = "Base corporativa de origem.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo é obrigatório")
    private TipoPessoaCorporativa tipo;

    @Schema(description = "Referência devolvida pela busca: matrícula (empregado) ou ColaboradorId (colaborador).",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Referência é obrigatória")
    private String referencia;

    @Schema(description = "Perfil a conceder.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Perfil é obrigatório")
    private Perfil perfil;

    @Schema(description = "UUID do laboratório-base, que deve pertencer à Unidade em operação.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Laboratório-base é obrigatório")
    private UUID laboratorioBaseId;
}
