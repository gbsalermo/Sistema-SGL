package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Registro de recebimento do resíduo pela gestão para início da conferência.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceberResiduoRequestDTO {

    @NotNull(message = "O usuário gestor é obrigatório")
    private UUID usuarioGestorId;

    private String observacao;
}
