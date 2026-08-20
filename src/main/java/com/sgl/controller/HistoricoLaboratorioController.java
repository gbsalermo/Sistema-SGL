package com.sgl.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.ConsumoProdutoLaboratorioResponseDTO;
import com.sgl.dto.response.HistoricoLaboratorioResponseDTO;
import com.sgl.service.HistoricoLaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Histórico de Laboratório", description = "Consultas do histórico de materiais recebidos e consumidos pelos laboratórios.")
@RestController
@RequestMapping("/api/v1/historico-laboratorio")
@RequiredArgsConstructor
public class HistoricoLaboratorioController {

    private final HistoricoLaboratorioService historicoLaboratorioService;

    @Operation(summary = "Listar histórico", description = "Retorna todos os registros do histórico de laboratório.")
    @GetMapping
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(historicoLaboratorioService.listarTodos());
    }

    @Operation(summary = "Buscar histórico por ID", description = "Retorna um registro de histórico pelo seu identificador público UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<HistoricoLaboratorioResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(historicoLaboratorioService.buscarPorId(id));
    }

    @Operation(summary = "Listar histórico por laboratório", description = "Retorna os registros de histórico vinculados ao laboratório informado.")
    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorLaboratorio(
            @PathVariable UUID laboratorioId) {
        return ResponseEntity.ok(
                historicoLaboratorioService.listarPorLaboratorio(laboratorioId)
        );
    }

    @Operation(summary = "Listar histórico por produto", description = "Retorna os registros de histórico vinculados ao produto informado.")
    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarProduto(
            @PathVariable UUID produtoId) {
        return ResponseEntity.ok(historicoLaboratorioService.listarPorProduto(produtoId));
    }

    @Operation(summary = "Listar histórico por pedido", description = "Retorna os registros de histórico gerados pelo pedido informado.")
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorPedido(
            @PathVariable UUID pedidoId) {
        return ResponseEntity.ok(historicoLaboratorioService.listarPorPedido(pedidoId));
    }

    @Operation(summary = "Listar histórico por período", description = "Retorna os registros de um laboratório dentro do intervalo de datas informado.")
    @GetMapping("/laboratorio/{laboratorioId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorPeriodo(
            @PathVariable UUID laboratorioId,
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

    @Operation(summary = "Calcular consumo de produto", description = "Calcula o consumo de um produto por um laboratório dentro do período informado.")
    @GetMapping("/laboratorio/{laboratorioId}/produto/{produtoId}/consumo")
    public ResponseEntity<ConsumoProdutoLaboratorioResponseDTO> calcularConsumoProduto(
            @PathVariable UUID laboratorioId,
            @PathVariable UUID produtoId,
            @RequestParam LocalDate dataInicio,
            @RequestParam LocalDate dataFim) {

        return ResponseEntity.ok(
                historicoLaboratorioService.calcularConsumoProduto(
                        laboratorioId,
                        produtoId,
                        dataInicio,
                        dataFim
                )
        );
    }

    @Operation(summary = "Listar histórico por projeto e período", description = "Retorna os registros de um laboratório vinculados a um projeto dentro do período informado.")
    @GetMapping("/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorProjetoEPeriodo(
            @PathVariable UUID laboratorioId,
            @PathVariable UUID projetoId,
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
