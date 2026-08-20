package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Resumo calculado do consumo de um produto por laboratório em um período.")
@Getter
@AllArgsConstructor
public class ConsumoProdutoLaboratorioResponseDTO {

    @Schema(description = "Identificador público UUID do laboratório.", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID laboratorioId;
    @Schema(description = "Nome do laboratório.", example = "Laboratório de Química Orgânica")
    private String laboratorioNome;
    @Schema(description = "Identificador público UUID do produto.", example = "550e8400-e29b-41d4-a716-446655440004")
    private UUID produtoId;
    @Schema(description = "Nome do produto.", example = "Extrato de DNA Plant Wizard")
    private String produtoNome;
    @Schema(description = "Unidade de armazenamento do produto.", example = "kit com 50 reações")
    private String produtoUnidadeArmazenamento;
    @Schema(description = "Data inicial considerada no cálculo.", example = "2026-08-01")
    private LocalDate dataInicio;
    @Schema(description = "Data final considerada no cálculo.", example = "2026-08-31")
    private LocalDate dataFim;
    @Schema(description = "Quantidade de pedidos considerados no período.", example = "4")
    private Long quantidadePedidos;
    @Schema(description = "Quantidade total efetivamente recebida no período.", example = "32")
    private Integer quantidadeTotalRecebida;
    @Schema(description = "Média de quantidade recebida por pedido.", example = "8.00")
    private BigDecimal mediaQuantidadePorPedido;
    @Schema(description = "Número de meses considerados no cálculo.", example = "1")
    private Integer mesesConsiderados;
    @Schema(description = "Média de consumo mensal calculada.", example = "32.00")
    private BigDecimal mediaConsumoMensal;
    @Schema(description = "Quantidade mínima sugerida com base no consumo calculado.", example = "32")
    private Integer quantidadeMinimaSugerida;
}
