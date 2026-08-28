package com.sgl.dto.request;

import java.time.LocalDate;

import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoEmbalagem;

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

    @Schema(description = "Tipo principal da embalagem recebida.", example = "KIT", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Tipo de embalagem é obrigatório")
    private TipoEmbalagem tipoEmbalagem;

    @Schema(description = "Especificação da embalagem recebida.", example = "kit com 50 unidades")
    private String apresentacao;

    @Schema(description = "Quantidade de embalagens/unidades físicas recebidas.", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Quantidade da entrada é obrigatória")
    @Min(value = 1, message = "Quantidade da entrada deve ser maior que zero")
    private Integer quantidade;

    @Schema(description = "Multiplicador de unidades individuais por embalagem. Ex.: kit de 50 -> 50.", example = "50", minimum = "1")
    @Min(value = 1, message = "Multiplicador deve ser maior que zero")
    private Integer conteudoPorApresentacao;

    @Schema(description = "Indica se a embalagem pode ser fracionada em uma saída parcial.", example = "true")
    private Boolean fracionavel;

    @Schema(description = "Data de validade do lote, quando aplicável ao produto.", example = "2027-08-31")
    private LocalDate dataValidade;

    @Schema(description = "Origem da movimentação de entrada.", example = "COMPRA", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Origem da entrada é obrigatória")
    private OrigemMovimentacao origem;

    @Schema(description = "Observação opcional sobre a entrada do lote.", example = "Material recebido conforme nota fiscal.")
    private String observacao;

    /** Compatibilidade com chamadas/testes anteriores. */
    public EntradaLoteRequestDTO(
            String numeroLote,
            Integer quantidade,
            LocalDate dataValidade,
            OrigemMovimentacao origem,
            String observacao) {
        this.numeroLote = numeroLote;
        this.tipoEmbalagem = TipoEmbalagem.UNITARIO;
        this.apresentacao = "unidade";
        this.quantidade = quantidade;
        this.conteudoPorApresentacao = 1;
        this.fracionavel = true;
        this.dataValidade = dataValidade;
        this.origem = origem;
        this.observacao = observacao;
    }
}
