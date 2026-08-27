package com.sgl.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Confirmação da saída do resíduo do armazenamento temporário para sua destinação final.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DespacharResiduoRequestDTO {

    @NotNull(message = "O usuário gestor é obrigatório")
    private UUID usuarioGestorId;

    @NotBlank(message = "O destino final confirmado é obrigatório")
    private String destinoFinalConfirmado;

    private String observacao;
}
