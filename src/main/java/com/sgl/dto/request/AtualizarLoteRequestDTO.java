package com.sgl.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Dados permitidos para atualização cadastral de um lote.")
@Getter
@Setter
public class AtualizarLoteRequestDTO {

    @Schema(description = "Número de identificação do lote.", example = "LOT-2026-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    @Schema(description = "Data de validade do lote, quando aplicável.", example = "2027-08-31")
    private LocalDate dataValidade;

    @Schema(description = "Indica se o lote está ativo.", example = "true")
    private Boolean ativo;
}
