package com.sgl.dto.request;

import java.util.UUID;

import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Aprovação de um usuário pendente: perfil confirmado e laboratório-base.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprovarUsuarioRequestDTO {

    @Schema(description = "Perfil a conceder.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Perfil é obrigatório")
    private Perfil perfil;

    @Schema(description = "UUID do laboratório-base, que deve pertencer à Unidade em operação.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Laboratório-base é obrigatório")
    private UUID laboratorioBaseId;
}
