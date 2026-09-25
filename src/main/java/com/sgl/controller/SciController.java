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

import com.sgl.dto.request.SciRequestDTO;
import com.sgl.dto.response.SciResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.SciService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "SCI", description = "Operações de cadastro, consulta e manutenção dos SCI vinculados aos projetos.")
@RestController
@RequestMapping("/api/v1/scis")
@RequiredArgsConstructor
public class SciController {

	private final SciService sciService;

	@Operation(summary = "Criar SCI", description = "Cadastra um novo SCI vinculado obrigatoriamente a um projeto da unidade atual.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "SCI criado com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PostMapping
	public ResponseEntity<SciResponseDTO> criar(@Valid @RequestBody SciRequestDTO dto) {

		SciResponseDTO criado = sciService.criar(dto);

		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@Operation(summary = "Listar SCI", description = "Retorna os SCI pertencentes aos projetos da unidade atual.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SCI listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Contexto de unidade não informado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping
	public ResponseEntity<List<SciResponseDTO>> listarTodos() {

		return ResponseEntity.ok(sciService.listarTodos());
	}

	@Operation(summary = "Buscar SCI por ID", description = "Retorna um SCI da unidade atual pelo seu identificador público UUID.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "SCI encontrado", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/{id}")
	public ResponseEntity<SciResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(sciService.buscarPorId(id));
	}

	@Operation(summary = "Listar SCI por projeto", description = "Retorna os SCI associados ao projeto informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SCI listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-projeto")
	public ResponseEntity<List<SciResponseDTO>> listarPorProjeto(@RequestParam UUID projetoId) {

		return ResponseEntity.ok(sciService.listarPorProjeto(projetoId));
	}

	@Operation(summary = "Listar SCI habilitados", description = "Retorna os SCI da unidade atual cujo indicador técnico ativo esteja habilitado. Este filtro é independente do status de negócio do SCI.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SCI habilitados listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/ativos")
	public ResponseEntity<List<SciResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(sciService.listarAtivos());
	}

	@Operation(summary = "Listar SCI habilitados por projeto", description = "Retorna os SCI tecnicamente ativos associados ao projeto informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SCI habilitados listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-projeto/ativos")
	public ResponseEntity<List<SciResponseDTO>> listarAtivosPorProjeto(@RequestParam UUID projetoId) {

		return ResponseEntity.ok(sciService.listarAtivosPorProjeto(projetoId));
	}

	@Operation(summary = "Atualizar SCI", description = "Atualiza os dados de um SCI preservando obrigatoriamente seu vínculo com o projeto original.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "SCI atualizado com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PutMapping("/{id}")
	public ResponseEntity<SciResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody SciRequestDTO dto) {

		return ResponseEntity.ok(sciService.atualizar(id, dto));
	}

	@Operation(summary = "Desativar SCI", description = "Desativa logicamente o SCI identificado pelo UUID informado, preservando seu registro.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "SCI desativado com sucesso"),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		sciService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}