package com.sgl.dto.request;

import java.util.List;

import com.sgl.model.enums.PapelSistema;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Papéis do sistema de um usuário existente. A lista substitui os papéis atuais; vazia remove todos.")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPapeisRequestDTO {

    @Schema(description = "Papéis a manter ou conceder.", example = "[\"GESTOR\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Papéis são obrigatórios")
    private List<@NotNull(message = "Papel inválido na lista") PapelSistema> papeis;
}
