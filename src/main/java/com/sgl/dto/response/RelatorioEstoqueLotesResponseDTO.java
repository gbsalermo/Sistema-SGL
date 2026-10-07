package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RelatorioEstoqueLotesResponseDTO {

    private LocalDateTime geradoEm;
    private Integer totalEstoques;
    private Integer estoquesAtivos;
    private Integer estoquesAbaixoMinimo;
    private BigDecimal quantidadeTotalEstoque;
    private Integer totalLotes;
    private Integer lotesAtivos;
    private Integer lotesVencidos;
    private Integer lotesProximosVencimento;
    private Integer lotesEsgotados;
    private List<EstoqueItem> estoques;
    private List<LoteItem> lotes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class EstoqueItem {
        private UUID estoqueId;
        private UUID unidadeId;
        private String unidadeNome;
        private String unidadeSigla;
        private UUID produtoId;
        private String produtoNome;
        private String codigoReferencia;
        private String unidadeMedida;
        private BigDecimal quantidadeAtual;
        private BigDecimal quantidadeMinima;
        private Boolean abaixoMinimo;
        private Boolean ativo;
        private Integer totalLotes;
        private Integer lotesAtivos;
        private Integer lotesVencidos;
        private Integer lotesProximosVencimento;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LoteItem {
        private UUID loteId;
        private UUID estoqueId;
        private UUID unidadeId;
        private String unidadeNome;
        private UUID produtoId;
        private String produtoNome;
        private String codigoInterno;
        private String numeroLote;
        private BigDecimal quantidadeInicial;
        private BigDecimal quantidadeDisponivel;
        private LocalDate dataEntrada;
        private LocalDate dataValidade;
        private Boolean ativo;
        private String situacao;
    }
}
