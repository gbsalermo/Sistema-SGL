package com.sgl.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioResumoOperacionalResponseDTO;
import com.sgl.service.RelatorioResumoOperacionalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios/resumo-operacional")
@RequiredArgsConstructor
public class RelatorioResumoOperacionalController {

    private final RelatorioResumoOperacionalService relatorioResumoOperacionalService;

    @Operation(
            summary = "Gerar resumo operacional",
            description = "Retorna totais e rankings das principais entradas, saídas e lotes movimentados."
    )
    @GetMapping
    public ResponseEntity<RelatorioResumoOperacionalResponseDTO> gerar(
            @RequestParam(required = false) UUID produtoId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) Integer limite) {

        return ResponseEntity.ok(
                relatorioResumoOperacionalService.gerar(
                        produtoId,
                        dataInicio,
                        dataFim,
                        limite
                )
        );
    }
}
