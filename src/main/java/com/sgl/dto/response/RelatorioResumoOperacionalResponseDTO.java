package com.sgl.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "Resumo gerencial das principais entradas, saídas e lotes movimentados.")
@Getter
@AllArgsConstructor
public class RelatorioResumoOperacionalResponseDTO {

    private LocalDateTime geradoEm;
    private Integer totalMovimentacoes;
    private Integer quantidadeEntradas;
    private Integer quantidadeSaidas;
    private Integer quantidadeDescartes;
    private Integer produtosMovimentados;
    private Integer lotesMovimentados;
    private List<ProdutoRanking> principaisEntradas;
    private List<ProdutoRanking> principaisSaidas;
    private List<LoteRanking> lotesMaisMovimentados;

    @Getter
    @AllArgsConstructor
    public static class ProdutoRanking {
        private UUID produtoId;
        private String produtoNome;
        private Integer quantidade;
        private Integer movimentacoes;
    }

    @Getter
    @AllArgsConstructor
    public static class LoteRanking {
        private UUID loteId;
        private String codigoInterno;
        private String numeroLote;
        private UUID produtoId;
        private String produtoNome;
        private Integer quantidadeMovimentada;
        private Integer movimentacoes;
        private Integer quantidadeEntradas;
        private Integer quantidadeSaidas;
        private Integer saldoAtual;
        private LocalDate dataValidade;
    }
}
