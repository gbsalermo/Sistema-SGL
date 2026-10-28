package com.sgl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Login de um usuário local de desenvolvimento (somente perfis dev e demo).")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevLoginRequestDTO {

    @Schema(description = "Login do usuário de exemplo.", example = "gestor.ib", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Login é obrigatório")
    private String login;
}
