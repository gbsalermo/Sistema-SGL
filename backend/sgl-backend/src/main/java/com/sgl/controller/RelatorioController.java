package com.sgl.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioEstagiariosResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.RelatorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @Operation(
            summary = "Gerar relatório de estagiários",
            description = "Retorna estagiários com filtros opcionais por situação, laboratório e período de vínculo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Período informado é inválido", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/estagiarios")
    public ResponseEntity<RelatorioEstagiariosResponseDTO> relatorioEstagiarios(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) UUID laboratorioId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {

        return ResponseEntity.ok(
                relatorioService.gerarRelatorioEstagiarios(
                        ativo,
                        laboratorioId,
                        dataInicio,
                        dataFim
                )
        );
    }
}
