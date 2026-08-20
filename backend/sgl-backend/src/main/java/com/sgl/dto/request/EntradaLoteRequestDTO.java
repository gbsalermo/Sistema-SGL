package com.sgl.dto.request;

import java.time.LocalDate;

import com.sgl.model.enums.OrigemMovimentacao;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados necessários para registrar a entrada de um lote no estoque.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntradaLoteRequestDTO {

    @Schema(description = "Número de identificação do lote informado pelo fornecedor ou responsável.", example = "LOT-2026-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Número do lote é obrigatório")
    private String numeroLote;

    @Schema(description = "Quantidade recebida no lote.", example = "20", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade da entrada é obrigatória")
    @Min(value = 1, message = "Quantidade da entrada deve ser maior que zero")
    private Integer quantidade;

    @Schema(description = "Data de validade do lote, quando aplicável ao produto.", example = "2027-08-31")
    private LocalDate dataValidade;

    @Schema(description = "Origem da movimentação de entrada.", example = "COMPRA", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Origem da entrada é obrigatória")
    private OrigemMovimentacao origem;

    @Schema(description = "Observação opcional sobre a entrada do lote.", example = "Material recebido conforme nota fiscal.")
    private String observacao;
}
