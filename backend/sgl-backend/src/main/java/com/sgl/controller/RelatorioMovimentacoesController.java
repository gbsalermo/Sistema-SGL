package com.sgl.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioMovimentacoesResponseDTO;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.service.RelatorioMovimentacoesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios/movimentacoes")
@RequiredArgsConstructor
public class RelatorioMovimentacoesController {

    private final RelatorioMovimentacoesService relatorioMovimentacoesService;

    @Operation(
            summary = "Gerar relatório de movimentações",
            description = "Consolida movimentações com filtros opcionais por tipo, origem, produto, laboratório, responsável, lote e período."
    )
    @GetMapping
    public ResponseEntity<RelatorioMovimentacoesResponseDTO> gerar(
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) OrigemMovimentacao origem,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) UUID loteId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {

        return ResponseEntity.ok(
                relatorioMovimentacoesService.gerar(
                        tipo,
                        origem,
                        produtoId,
                        laboratorioId,
                        usuarioId,
                        loteId,
                        dataInicio,
                        dataFim
                )
        );
    }
}
