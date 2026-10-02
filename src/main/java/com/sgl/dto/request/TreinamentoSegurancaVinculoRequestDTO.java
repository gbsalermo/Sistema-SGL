package com.sgl.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreinamentoSegurancaVinculoRequestDTO {

    @NotNull(message = "Situação do treinamento é obrigatória")
    private Boolean concluido;

    @NotNull(message = "Usuário operador é obrigatório")
    private UUID usuarioId;

    @Size(max = 1000, message = "Observação deve possuir no máximo 1000 caracteres")
    private String observacao;
}
