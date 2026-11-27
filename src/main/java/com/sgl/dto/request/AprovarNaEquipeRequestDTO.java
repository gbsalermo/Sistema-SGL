package com.sgl.dto.request;

import com.sgl.model.enums.CategoriaCargo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aprovação de um pendente pela equipe de um laboratório. Nunca concede papéis do sistema.")
public record AprovarNaEquipeRequestDTO(
        @Schema(description = "Categoria confirmada; nula mantém a sugerida no primeiro login.") CategoriaCargo categoriaCargo) {
}
