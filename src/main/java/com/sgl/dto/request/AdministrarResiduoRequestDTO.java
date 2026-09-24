package com.sgl.dto.request;

import java.util.UUID;

import com.sgl.model.enums.AcaoAdministrativaResiduo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdministrarResiduoRequestDTO {

    @NotNull(message = "O usuário administrador é obrigatório")
    private UUID usuarioAdministradorId;

    @NotNull(message = "A ação administrativa é obrigatória")
    private AcaoAdministrativaResiduo acao;

    @NotBlank(message = "A justificativa da ação administrativa é obrigatória")
    @Size(max = 1000, message = "A justificativa deve possuir no máximo 1000 caracteres")
    private String justificativa;
}
