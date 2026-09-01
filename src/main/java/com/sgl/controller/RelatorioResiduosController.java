package com.sgl.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import com.sgl.dto.response.RelatorioResiduosResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.StatusResiduo;
import com.sgl.service.RelatorioResiduosExportacaoService;
import com.sgl.service.RelatorioResiduosService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/relatorios/residuos")
@RequiredArgsConstructor
@Tag(name = "Relatórios - Resíduos", description = "Consulta e exportação da rastreabilidade de resíduos laboratoriais.")
public class RelatorioResiduosController {

    private final RelatorioResiduosService relatorioService;
    private final RelatorioResiduosExportacaoService exportacaoService;

    @GetMapping
    @Operation(summary = "Gerar relatório de resíduos")
    public ResponseEntity<RelatorioResiduosResponseDTO> gerar(
            @RequestParam(required = false) StatusResiduo status,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) NivelRisco nivelRisco,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return ResponseEntity.ok(relatorioService.gerar(status, laboratorioId, nivelRisco, dataInicio, dataFim));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exportar relatório de resíduos em PDF ou XLSX")
    public ResponseEntity<byte[]> exportar(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam(required = false) StatusResiduo status,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) NivelRisco nivelRisco,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        var relatorio = relatorioService.gerar(status, laboratorioId, nivelRisco, dataInicio, dataFim);
        ArquivoRelatorioDTO arquivo = exportacaoService.exportar(relatorio, formato);

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
