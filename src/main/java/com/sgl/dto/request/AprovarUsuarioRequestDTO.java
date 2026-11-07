package com.sgl.dto.request;

import java.util.List;
import java.util.UUID;

import com.sgl.model.enums.Perfil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Aprovação de um usuário pendente: perfil confirmado e laboratórios.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprovarUsuarioRequestDTO {

    @Schema(description = "Perfil a conceder.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Perfil é obrigatório")
    private Perfil perfil;

    @Schema(description = "UUIDs dos laboratórios da pessoa, que devem pertencer à Unidade em operação. "
            + "Obrigatório para perfis operacionais; a gestão pode ficar sem laboratório.")
    private List<@NotNull(message = "Laboratório inválido na lista") UUID> laboratorioIds;
}
