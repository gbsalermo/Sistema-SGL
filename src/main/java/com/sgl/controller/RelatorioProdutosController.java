package com.sgl.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioProdutosResponseDTO;
import com.sgl.model.enums.NivelRisco;
import com.sgl.model.enums.OrgaoFiscalizador;
import com.sgl.service.RelatorioProdutosService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios/produtos")
@RequiredArgsConstructor
public class RelatorioProdutosController {

    private final RelatorioProdutosService relatorioProdutosService;

    @Operation(
            summary = "Gerar relatório de produtos",
            description = "Retorna o catálogo de produtos com filtros por situação, fiscalização, perecibilidade, nível de risco e órgão fiscalizador."
    )
    @GetMapping
    public ResponseEntity<RelatorioProdutosResponseDTO> gerar(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) Boolean fiscalizado,
            @RequestParam(required = false) Boolean perecivel,
            @RequestParam(required = false) NivelRisco risco,
            @RequestParam(required = false) OrgaoFiscalizador orgaoFiscalizador) {

        return ResponseEntity.ok(
                relatorioProdutosService.gerar(
                        ativo,
                        fiscalizado,
                        perecivel,
                        risco,
                        orgaoFiscalizador
                )
        );
    }
}
