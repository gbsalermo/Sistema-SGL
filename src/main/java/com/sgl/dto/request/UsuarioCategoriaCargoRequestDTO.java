package com.sgl.dto.request;

import com.sgl.model.enums.CategoriaCargo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Ajuste da categoria de cargo, usado quando o cargo corporativo não a define (ex.: bolsista).")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCategoriaCargoRequestDTO {

    @Schema(description = "Nova categoria de cargo.", example = "ESTAGIARIO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Categoria de cargo é obrigatória")
    private CategoriaCargo categoriaCargo;
}
