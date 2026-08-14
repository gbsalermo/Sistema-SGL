package com.sgl.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumoProdutoLaboratorioDTO {
    private UUID laboratorioId;
    private String laboratorioNome;
    private UUID produtoId;
    private String produtoNome;
    private String produtoUnidadeArmazenamento;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Long quantidadePedidos;
    private Integer quantidadeTotalRecebida;
    private BigDecimal mediaQuantidadePorPedido;
    private Integer mesesConsiderados;
    private BigDecimal mediaConsumoMensal;
    private Integer quantidadeMinimaSugerida;
}
