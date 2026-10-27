package com.sgl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Schema(description = "Credenciais corporativas da Embrapa para entrar no SGL.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @Schema(description = "Login de rede (ex.: m344136) ou CPF.", example = "m344136", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Login é obrigatório")
    private String login;

    @Schema(description = "Senha da conta Embrapa. Não é armazenada pelo SGL.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Senha é obrigatória")
    @ToString.Exclude // never let the password reach a log through toString()
    private String senha;
}
