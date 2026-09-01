package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Confirmação do armazenamento temporário do resíduo já conferido e rotulado.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArmazenarResiduoRequestDTO {

    @NotNull(message = "O usuário gestor é obrigatório")
    private UUID usuarioGestorId;

    @Schema(description = "Permite corrigir o local previamente definido no momento da confirmação física.")
    private String localArmazenamentoTemporario;
}
