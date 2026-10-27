package com.sgl.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Opções de autenticação disponíveis neste ambiente.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthConfigResponseDTO {

    @Schema(description = "Indica se o login DEV (sem senha) está disponível. Só em dev e demo.", example = "false")
    private boolean devLoginAtivo;
}
