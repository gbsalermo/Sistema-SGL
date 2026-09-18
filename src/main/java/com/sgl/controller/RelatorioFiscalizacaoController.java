package com.sgl.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioFiscalizacaoResponseDTO;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.service.RelatorioFiscalizacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para gestão, auditoria e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioFiscalizacaoController {

    private final RelatorioFiscalizacaoService relatorioFiscalizacaoService;

    @Operation(
            summary = "Gerar relatório de fiscalização",
            description = "Retorna somente produtos explicitamente classificados como fiscalizados, com saldo, lotes, vencimentos e trilha de movimentações."
    )
    @GetMapping("/fiscalizacao")
    public ResponseEntity<RelatorioFiscalizacaoResponseDTO> gerar(
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) OrgaoFiscalizador orgaoFiscalizador,
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false, defaultValue = "30") Integer diasVencimento) {

        return ResponseEntity.ok(relatorioFiscalizacaoService.gerar(
                produtoId,
                orgaoFiscalizador,
                unidadeId,
                dataInicio,
                dataFim,
                diasVencimento
        ));
    }
}
