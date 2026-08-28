package com.sgl.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.TipoMovimentacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RelatorioFiscalizacaoResponseDTO {

    private LocalDateTime geradoEm;
    private Integer totalProdutosFiscalizados;
    private Integer saldoAtualTotal;
    private Integer lotesAtivos;
    private Integer lotesVencidos;
    private Integer lotesProximosVencimento;
    private Integer quantidadeEntradas;
    private Integer quantidadeSaidas;
    private List<ProdutoFiscalizadoItem> produtos;
    private List<MovimentacaoFiscalizadaItem> movimentacoes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProdutoFiscalizadoItem {
        private UUID produtoId;
        private String produtoNome;
        private String codigoReferencia;
        private Set<OrgaoFiscalizador> orgaosFiscalizadores;
        private String observacaoFiscalizacao;
        private Integer saldoAtual;
        private Integer lotesAtivos;
        private Integer lotesVencidos;
        private Integer lotesProximosVencimento;
        private LocalDate proximoVencimento;
        private Integer quantidadeEntradas;
        private Integer quantidadeSaidas;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MovimentacaoFiscalizadaItem {
        private UUID movimentacaoId;
        private LocalDateTime dataMovimentacao;
        private UUID produtoId;
        private String produtoNome;
        private TipoMovimentacao tipoMovimentacao;
        private Integer quantidadeMovimentada;
        private UUID loteId;
        private String codigoInternoLote;
        private String numeroLote;
        private LocalDate dataValidadeLote;
        private UUID laboratorioId;
        private String laboratorioNome;
        private UUID projetoId;
        private String projetoNome;
        private UUID solicitanteId;
        private String solicitanteNome;
        private UUID pedidoId;
        private UUID responsavelId;
        private String responsavelNome;
        private Integer saldoAposMovimentacao;
    }
}
