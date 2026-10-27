package com.sgl.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Resumo gerencial das principais entradas, saídas e lotes movimentados.")
@Getter
@AllArgsConstructor
public class RelatorioResumoOperacionalResponseDTO {

    private LocalDateTime geradoEm;
    private Integer totalMovimentacoes;
    private BigDecimal quantidadeEntradas;
    private BigDecimal quantidadeSaidas;
    private BigDecimal quantidadeDescartes;
    private Integer produtosMovimentados;
    private Integer lotesMovimentados;
    private List<ProdutoRanking> principaisEntradas;
    private List<ProdutoRanking> principaisSaidas;
    private List<LoteRanking> lotesMaisMovimentados;

    @Setter private Integer totalEntradas;
    @Setter private Integer totalSaidas;
    @Setter private Integer totalDevolucoes;
    @Setter private Integer totalAjustes;
    @Setter private Integer totalDescartes;

    public RelatorioResumoOperacionalResponseDTO(LocalDateTime geradoEm, Integer totalMovimentacoes,
            BigDecimal quantidadeEntradas, BigDecimal quantidadeSaidas, BigDecimal quantidadeDescartes,
            Integer produtosMovimentados, Integer lotesMovimentados,
            List<ProdutoRanking> principaisEntradas, List<ProdutoRanking> principaisSaidas,
            List<LoteRanking> lotesMaisMovimentados) {
        this(geradoEm, totalMovimentacoes, quantidadeEntradas, quantidadeSaidas, quantidadeDescartes,
                produtosMovimentados, lotesMovimentados, principaisEntradas, principaisSaidas,
                lotesMaisMovimentados, null, null, null, null, null);
    }

    @Getter
    @AllArgsConstructor
    public static class ProdutoRanking {
        private UUID produtoId;
        private String produtoNome;
        private BigDecimal quantidade;
        private Integer movimentacoes;
        private String unidadeMedida;

        public ProdutoRanking(UUID produtoId, String produtoNome, BigDecimal quantidade, Integer movimentacoes) {
            this(produtoId, produtoNome, quantidade, movimentacoes, null);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class LoteRanking {
        private UUID loteId;
        private String codigoInterno;
        private String numeroLote;
        private UUID produtoId;
        private String produtoNome;
        private BigDecimal quantidadeMovimentada;
        private Integer movimentacoes;
        private BigDecimal quantidadeEntradas;
        private BigDecimal quantidadeSaidas;
        private BigDecimal saldoAtual;
        private LocalDate dataValidade;
        private String unidadeMedida;

        public LoteRanking(UUID loteId, String codigoInterno, String numeroLote, UUID produtoId,
                String produtoNome, BigDecimal quantidadeMovimentada, Integer movimentacoes,
                BigDecimal quantidadeEntradas, BigDecimal quantidadeSaidas, BigDecimal saldoAtual,
                LocalDate dataValidade) {
            this(loteId, codigoInterno, numeroLote, produtoId, produtoNome, quantidadeMovimentada,
                    movimentacoes, quantidadeEntradas, quantidadeSaidas, saldoAtual, dataValidade, null);
        }
    }
}
