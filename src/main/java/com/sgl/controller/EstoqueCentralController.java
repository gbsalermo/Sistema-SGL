package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.EstoqueCentralRequestDTO;
import com.sgl.dto.response.EstoqueCentralResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.EstoqueCentralService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Estoque Central", description = "Operações de consulta e gerenciamento do estoque central por unidade e produto.")
@RestController
@RequestMapping("/api/v1/estoque-central")
@RequiredArgsConstructor
public class EstoqueCentralController {

    private final EstoqueCentralService estoqueCentralService;

    @Operation(summary = "Criar estoque central", description = "Cria um registro de estoque central para um produto em uma unidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estoque central criado com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Unidade ou produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<EstoqueCentralResponseDTO> criar(@Valid @RequestBody EstoqueCentralRequestDTO dto) {
        EstoqueCentralResponseDTO salvo = estoqueCentralService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Operation(summary = "Listar estoques", description = "Retorna todos os registros de estoque central cadastrados no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoques listados com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarTodos() {
        return ResponseEntity.ok(estoqueCentralService.listarTodos());
    }

    @Operation(summary = "Buscar estoque por ID", description = "Retorna um registro de estoque central pelo seu identificador público UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EstoqueCentralResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(estoqueCentralService.buscarPorId(id));
    }

    @Operation(summary = "Listar estoques por unidade", description = "Retorna os registros de estoque central vinculados à unidade informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoques listados com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-unidade")
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarPorUnidade(@RequestParam UUID unidadeId) {
        return ResponseEntity.ok(estoqueCentralService.listarPorUnidade(unidadeId));
    }

    @Operation(summary = "Buscar estoque por unidade e produto", description = "Retorna o registro de estoque correspondente à unidade e ao produto informados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque encontrado", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Estoque, unidade ou produto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/por-unidade-produto")
    public ResponseEntity<EstoqueCentralResponseDTO> buscarPorUnidadeEProduto(@RequestParam UUID unidadeId, @RequestParam UUID produtoId) {
        return ResponseEntity.ok(estoqueCentralService.buscarPorUnidadeEProduto(unidadeId, produtoId));
    }

    @Operation(summary = "Atualizar estoque central", description = "Atualiza os dados configuráveis do registro de estoque central informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Estoque ou recurso relacionado não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EstoqueCentralResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody EstoqueCentralRequestDTO dto) {
        return ResponseEntity.ok(estoqueCentralService.atualizar(id, dto));
    }

    @Operation(summary = "Excluir estoque central", description = "Remove o registro de estoque central identificado pelo UUID informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estoque excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estoque não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        estoqueCentralService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar estoques baixos", description = "Retorna os estoques da unidade cuja quantidade atual está abaixo da quantidade mínima configurada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoques baixos listados com sucesso", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<EstoqueCentralResponseDTO>> listarEstoqueBaixo(@RequestParam UUID unidadeId) {
        return ResponseEntity.ok(estoqueCentralService.listarEstoqueBaixoPorUnidade(unidadeId));
    }
}
