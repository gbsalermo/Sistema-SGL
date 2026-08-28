package com.sgl.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Dados permitidos para atualização cadastral de um lote. O código interno SGL não faz parte deste contrato porque é imutável.")
@Getter
@Setter
public class AtualizarLoteRequestDTO {

    @Schema(description = "Número ou referência externa informada pelo fornecedor/responsável.", example = "FAB-2026-8841", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Número do lote do fornecedor é obrigatório")
    private String numeroLote;

    @Schema(description = "Nome da apresentação física do lote.", example = "kit")
    private String apresentacao;

    @Schema(description = "Indica se a apresentação permite saída parcial.", example = "true")
    private Boolean fracionavel;

    @Schema(description = "Observação cadastral do lote.", example = "Material recebido lacrado.")
    private String observacao;

    @Schema(description = "Data de validade do lote, quando aplicável.", example = "2027-08-31")
    private LocalDate dataValidade;

    @Schema(description = "Indica se o lote está ativo.", example = "true")
    private Boolean ativo;
}
