package com.sgl.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioEstoqueLotesResponseDTO;
import com.sgl.service.RelatorioEstoqueLotesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios/estoque-lotes")
@RequiredArgsConstructor
public class RelatorioEstoqueLotesController {

    private final RelatorioEstoqueLotesService relatorioEstoqueLotesService;

    @Operation(
            summary = "Gerar relatório de estoque e lotes",
            description = "Consolida posição atual de estoque, mínimos, lotes ativos, vencidos, próximos do vencimento e esgotados."
    )
    @GetMapping
    public ResponseEntity<RelatorioEstoqueLotesResponseDTO> gerar(
            @RequestParam(required = false) UUID unidadeId,
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) Boolean ativoEstoque,
            @RequestParam(required = false) Boolean abaixoMinimo,
            @RequestParam(required = false) Boolean ativoLote,
            @RequestParam(required = false) String validade,
            @RequestParam(required = false, defaultValue = "30") Integer diasVencimento) {

        return ResponseEntity.ok(
                relatorioEstoqueLotesService.gerar(
                        unidadeId,
                        produtoId,
                        ativoEstoque,
                        abaixoMinimo,
                        ativoLote,
                        validade,
                        diasVencimento
                )
        );
    }
}
