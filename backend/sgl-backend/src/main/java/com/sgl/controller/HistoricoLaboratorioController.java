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
import com.sgl.exception.ApiError;
import com.sgl.service.HistoricoLaboratorioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Histórico de Laboratório", description = "Consultas do histórico de materiais recebidos e consumidos pelos laboratórios.")
@RestController
@RequestMapping("/api/v1/historico-laboratorio")
@RequiredArgsConstructor
public class HistoricoLaboratorioController {

    private final HistoricoLaboratorioService historicoLaboratorioService;

    @Operation(summary = "Listar histórico", description = "Retorna todos os registros do histórico de laboratório.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarTodos() { return ResponseEntity.ok(historicoLaboratorioService.listarTodos()); }

    @Operation(summary = "Buscar histórico por ID", description = "Retorna um registro de histórico pelo seu identificador público UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Registro encontrado"), @ApiResponse(responseCode = "404", description = "Registro não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<HistoricoLaboratorioResponseDTO> buscarPorId(@PathVariable UUID id) { return ResponseEntity.ok(historicoLaboratorioService.buscarPorId(id)); }

    @Operation(summary = "Listar histórico por laboratório", description = "Retorna os registros de histórico vinculados ao laboratório informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/laboratorio/{laboratorioId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorLaboratorio(@PathVariable UUID laboratorioId) { return ResponseEntity.ok(historicoLaboratorioService.listarPorLaboratorio(laboratorioId)); }

    @Operation(summary = "Listar histórico por produto", description = "Retorna os registros de histórico vinculados ao produto informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarProduto(@PathVariable UUID produtoId) { return ResponseEntity.ok(historicoLaboratorioService.listarPorProduto(produtoId)); }

    @Operation(summary = "Listar histórico por pedido", description = "Retorna os registros de histórico gerados pelo pedido informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorPedido(@PathVariable UUID pedidoId) { return ResponseEntity.ok(historicoLaboratorioService.listarPorPedido(pedidoId)); }

    @Operation(summary = "Listar histórico por período", description = "Retorna os registros de um laboratório dentro do intervalo de datas informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "400", description = "Período inválido", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/laboratorio/{laboratorioId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorPeriodo(@PathVariable UUID laboratorioId, @RequestParam LocalDate dataInicio, @RequestParam LocalDate dataFim) { return ResponseEntity.ok(historicoLaboratorioService.listarPorPeriodo(laboratorioId, dataInicio, dataFim)); }

    @Operation(summary = "Calcular consumo de produto", description = "Calcula o consumo de um produto por um laboratório dentro do período informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Consumo calculado com sucesso"), @ApiResponse(responseCode = "400", description = "Período inválido", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Laboratório ou produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/laboratorio/{laboratorioId}/produto/{produtoId}/consumo")
    public ResponseEntity<ConsumoProdutoLaboratorioResponseDTO> calcularConsumoProduto(@PathVariable UUID laboratorioId, @PathVariable UUID produtoId, @RequestParam LocalDate dataInicio, @RequestParam LocalDate dataFim) { return ResponseEntity.ok(historicoLaboratorioService.calcularConsumoProduto(laboratorioId, produtoId, dataInicio, dataFim)); }

    @Operation(summary = "Listar histórico por projeto e período", description = "Retorna os registros de um laboratório vinculados a um projeto dentro do período informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Histórico listado com sucesso"), @ApiResponse(responseCode = "400", description = "Período inválido ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Laboratório ou projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/laboratorio/{laboratorioId}/projeto/{projetoId}/periodo")
    public ResponseEntity<List<HistoricoLaboratorioResponseDTO>> listarPorProjetoEPeriodo(@PathVariable UUID laboratorioId, @PathVariable UUID projetoId, @RequestParam LocalDate dataInicio, @RequestParam LocalDate dataFim) { return ResponseEntity.ok(historicoLaboratorioService.listarPorProjetoEPeriodo(laboratorioId, projetoId, dataInicio, dataFim)); }
}
