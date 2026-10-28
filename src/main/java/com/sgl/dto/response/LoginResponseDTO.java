package com.sgl.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Sessão emitida pelo SGL após um login bem-sucedido.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    @Schema(description = "JWT a enviar no header Authorization: Bearer.")
    private String token;

    @Schema(description = "Instante em que o token expira.", example = "2026-10-27T20:00:00Z")
    private Instant expiraEm;

    @Schema(description = "Usuário da sessão.")
    private UsuarioSessaoResponseDTO usuario;
}
