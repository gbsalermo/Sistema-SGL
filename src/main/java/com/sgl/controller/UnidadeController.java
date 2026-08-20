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
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.UnidadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Unidades", description = "Operações de cadastro e consulta das unidades institucionais.")
@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @Operation(summary = "Listar unidades", description = "Retorna todas as unidades institucionais cadastradas no sistema.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Unidades listadas com sucesso"), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping
    public ResponseEntity<List<UnidadeResponseDTO>> listarTodos() { return ResponseEntity.ok(unidadeService.listarTodos()); }

    @Operation(summary = "Buscar unidade por ID", description = "Retorna uma unidade pelo seu identificador público UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Unidade encontrada"), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> buscarPorId(@PathVariable UUID id) { return ResponseEntity.ok(unidadeService.buscarPorId(id)); }

    @Operation(summary = "Criar unidade", description = "Cadastra uma nova unidade institucional no sistema.")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Unidade criada com sucesso"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping
    public ResponseEntity<UnidadeResponseDTO> criar(@Valid @RequestBody UnidadeRequestDTO dto) {
        UnidadeResponseDTO novaUnidade = unidadeService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novaUnidade);
    }

    @Operation(summary = "Atualizar unidade", description = "Atualiza os dados da unidade identificada pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Unidade atualizada com sucesso"), @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PutMapping("/{id}")
    public ResponseEntity<UnidadeResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody UnidadeRequestDTO dto) { return ResponseEntity.ok(unidadeService.atualizar(id, dto)); }

    @Operation(summary = "Excluir unidade", description = "Remove a unidade identificada pelo UUID informado.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Unidade excluída com sucesso"), @ApiResponse(responseCode = "404", description = "Unidade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))), @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) { unidadeService.deletar(id); return ResponseEntity.noContent().build(); }
}
