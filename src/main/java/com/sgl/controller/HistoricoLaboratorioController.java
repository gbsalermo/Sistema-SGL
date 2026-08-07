package com.sgl.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.HistoricoLaboratorioDTO;
import com.sgl.service.HistoricoLaboratorioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/historico-laboratorio")
@RequiredArgsConstructor
public class HistoricoLaboratorioController {

    private final HistoricoLaboratorioService historicoLaboratorioService;

    @GetMapping
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarTodos() {
        return ResponseEntity.ok(historicoLaboratorioService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoricoLaboratorioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(historicoLaboratorioService.buscarPorId(id));
    }

    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarPorLaboratorio(
            @PathVariable Long laboratorioId) {
        return ResponseEntity.ok(
                historicoLaboratorioService.listarPorLaboratorio(laboratorioId)
        );
    }

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarProduto(
            @PathVariable Long produtoId) {
        return ResponseEntity.ok(historicoLaboratorioService.listarPorProduto(produtoId));
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarPorPedido(
            @PathVariable Long pedidoId) {
        return ResponseEntity.ok(historicoLaboratorioService.listarPorPedido(pedidoId));
    }

    @GetMapping("/laboratorio/{laboratorioId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarPorPeriodo(
            @PathVariable Long laboratorioId,
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {

        return ResponseEntity.ok(
                historicoLaboratorioService.listarPorPeriodo(
                        laboratorioId,
                        dataInicio,
                        dataFim
                )
        );
    }

    /**
     * Materiais efetivamente recebidos por um projeto dentro do laboratório no
     * período informado.
     */
    @GetMapping("/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioDTO>> listarPorProjetoEPeriodo(
            @PathVariable Long laboratorioId,
            @PathVariable Long projetoId,
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {

        return ResponseEntity.ok(
                historicoLaboratorioService.listarPorProjetoEPeriodo(
                        laboratorioId,
                        projetoId,
                        dataInicio,
                        dataFim
                )
        );
    }
}
