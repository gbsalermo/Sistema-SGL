package com.sgl.controller;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.ArquivoRelatorioDTO;
import com.sgl.dto.response.RelatorioPessoasLaboratorioResponseDTO;
import com.sgl.model.enums.FormatoExportacaoRelatorio;
import com.sgl.model.enums.Perfil;
import com.sgl.service.RelatorioPessoasLaboratorioExportacaoService;
import com.sgl.service.RelatorioPessoasLaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/relatorios/pessoas-laboratorio")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
public class RelatorioPessoasLaboratorioController {

    private final RelatorioPessoasLaboratorioService relatorioService;
    private final RelatorioPessoasLaboratorioExportacaoService exportacaoService;

    @GetMapping
    @Operation(summary = "Lista as pessoas vinculadas a um laboratório")
    public ResponseEntity<RelatorioPessoasLaboratorioResponseDTO> gerar(
            @RequestParam UUID laboratorioId,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean ativo) {

        return ResponseEntity.ok(relatorioService.gerar(laboratorioId, perfil, ativo));
    }

    @GetMapping("/exportar")
    @Operation(summary = "Exporta as pessoas vinculadas a um laboratório em PDF ou XLSX")
    public ResponseEntity<byte[]> exportar(
            @RequestParam FormatoExportacaoRelatorio formato,
            @RequestParam UUID laboratorioId,
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(required = false) Boolean ativo) {

        var relatorio = relatorioService.gerar(laboratorioId, perfil, ativo);
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
