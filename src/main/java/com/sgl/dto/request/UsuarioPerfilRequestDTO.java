package com.sgl.dto.request;

import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Alteração administrativa do perfil de acesso de um usuário existente.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPerfilRequestDTO {

    @Schema(description = "Novo perfil de acesso do usuário.", example = "GESTOR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Perfil é obrigatório")
    private Perfil perfil;
}
