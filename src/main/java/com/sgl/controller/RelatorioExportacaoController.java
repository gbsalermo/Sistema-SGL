package com.sgl.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.service.RelatorioEstoqueLotesService;
import com.sgl.service.RelatorioExportacaoService;
import com.sgl.service.RelatorioFiscalizacaoService;
import com.sgl.service.RelatorioMovimentacoesService;
import com.sgl.service.RelatorioProdutosService;
import com.sgl.service.RelatorioResumoOperacionalService;
import com.sgl.service.RelatorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios - Exportação", description = "Exportação individual de relatórios do SGL em PDF ou XLSX.")
public class RelatorioExportacaoController {

    private final RelatorioService relatorioService;
    private final RelatorioProdutosService produtosService;
    private final RelatorioMovimentacoesService movimentacoesService;
    private final RelatorioResumoOperacionalService resumoService;
    private final RelatorioEstoqueLotesService estoqueLotesService;
    private final RelatorioFiscalizacaoService fiscalizacaoService;
    private final RelatorioExportacaoService exportacaoService;

    @GetMapping("/estagiarios/exportar")
    @Operation(summary = "Exporta o relatório de estagiários")
    public ResponseEntity<byte[]> estagiarios(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        var relatorio = relatorioService.gerarRelatorioEstagiarios(ativo, laboratorioId, dataInicio, dataFim);
        var arquivo = exportacaoService.exportarEstagiarios(relatorio, formato,
                filtros("Situação", ativo == null ? null : ativo ? "Ativos" : "Inativos",
                        "Laboratório", laboratorioId, "Data inicial", dataInicio, "Data final", dataFim));
        return resposta(arquivo);
    }

    @GetMapping("/produtos/exportar")
    @Operation(summary = "Exporta o relatório de produtos")
    public ResponseEntity<byte[]> produtos(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean fiscalizado,
            @RequestParam(required = false) Boolean perecivel,
            @RequestParam(required = false) NivelRisco risco,
            @RequestParam(required = false) OrgaoFiscalizador orgaoFiscalizador) {

        var relatorio = produtosService.gerar(ativo, fiscalizado, perecivel, risco, orgaoFiscalizador);
        var arquivo = exportacaoService.exportarProdutos(relatorio, formato,
                filtros("Situação", ativo == null ? null : ativo ? "Ativos" : "Inativos",
                        "Fiscalizado", simNao(fiscalizado), "Perecível", simNao(perecivel),
                        "Risco", risco, "Órgão", orgaoFiscalizador));
        return resposta(arquivo);
    }

    @GetMapping("/movimentacoes/exportar")
    @Operation(summary = "Exporta o relatório de movimentações")
    public ResponseEntity<byte[]> movimentacoes(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) OrigemMovimentacao origem,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID loteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        var relatorio = movimentacoesService.gerar(tipo, origem, produtoId, laboratorioId, usuarioId, loteId, dataInicio, dataFim);
        var arquivo = exportacaoService.exportarMovimentacoes(relatorio, formato,
                filtros("Tipo", tipo, "Origem", origem, "Produto", produtoId, "Laboratório", laboratorioId,
                        "Responsável", usuarioId, "Lote", loteId, "Data inicial", dataInicio, "Data final", dataFim));
        return resposta(arquivo);
    }

    @GetMapping("/resumo-operacional/exportar")
    @Operation(summary = "Exporta o resumo operacional")
    public ResponseEntity<byte[]> resumoOperacional(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Integer limite) {

        var relatorio = resumoService.gerar(produtoId, dataInicio, dataFim, limite);
        var arquivo = exportacaoService.exportarResumoOperacional(relatorio, formato,
                filtros("Produto", produtoId, "Data inicial", dataInicio, "Data final", dataFim, "Ranking", limite));
        return resposta(arquivo);
    }

    @GetMapping("/estoque-lotes/exportar")
    @Operation(summary = "Exporta o relatório de estoque e lotes")
    public ResponseEntity<byte[]> estoqueLotes(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) Boolean ativoEstoque,
            @RequestParam(required = false) Boolean abaixoMinimo,
            @RequestParam(required = false) Boolean ativoLote,
            @RequestParam(required = false) String validade,
            @RequestParam(required = false) Integer diasVencimento) {

        var relatorio = estoqueLotesService.gerar(unidadeId, produtoId, ativoEstoque, abaixoMinimo, ativoLote, validade, diasVencimento);
        var arquivo = exportacaoService.exportarEstoqueLotes(relatorio, formato,
                filtros("Unidade", unidadeId, "Produto", produtoId,
                        "Estoque", ativoEstoque == null ? null : ativoEstoque ? "Ativo" : "Inativo",
                        "Abaixo do mínimo", simNao(abaixoMinimo), "Lote ativo", simNao(ativoLote),
                        "Validade", validade, "Janela de vencimento", diasVencimento == null ? null : diasVencimento + " dias"));
        return resposta(arquivo);
    }

    @GetMapping("/fiscalizacao/exportar")
    @Operation(summary = "Exporta o relatório de fiscalização")
    public ResponseEntity<byte[]> fiscalizacao(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) OrgaoFiscalizador orgaoFiscalizador,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Integer diasVencimento) {

        var relatorio = fiscalizacaoService.gerar(produtoId, orgaoFiscalizador, unidadeId, dataInicio, dataFim, diasVencimento);
        var arquivo = exportacaoService.exportarFiscalizacao(relatorio, formato,
                filtros("Produto", produtoId, "Órgão", orgaoFiscalizador, "Unidade", unidadeId,
                        "Data inicial", dataInicio, "Data final", dataFim,
                        "Janela de vencimento", diasVencimento == null ? null : diasVencimento + " dias"));
        return resposta(arquivo);
    }

    private List<String> filtros(Object... valores) {
        return exportacaoService.filtros(valores);
    }

    private String simNao(Boolean valor) {
        return valor == null ? null : valor ? "Sim" : "Não";
    }

    private ResponseEntity<byte[]> resposta(ArquivoRelatorioDTO arquivo) {
        String contentDisposition = ContentDisposition.attachment()
                .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentLength(arquivo.conteudo().length)
                .body(arquivo.conteudo());
    }
}
