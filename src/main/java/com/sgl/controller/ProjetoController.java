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

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.ProjetoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Projetos", description = "Operações de cadastro, consulta e manutenção dos projetos da unidade atual.")
@RestController
@RequestMapping("/api/v1/projetos")
@RequiredArgsConstructor
public class ProjetoController {

	private final ProjetoService projetoService;

	@Operation(summary = "Criar projeto", description = "Cadastra um novo projeto associado ao laboratório responsável/contextual da unidade atual.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Projeto criado com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PostMapping
	public ResponseEntity<ProjetoResponseDTO> criar(@Valid @RequestBody ProjetoRequestDTO dto) {
		ProjetoResponseDTO criado = projetoService.criar(dto);
		return ResponseEntity.status(HttpStatus.CREATED).body(criado);
	}

	@Operation(summary = "Listar projetos", description = "Retorna os projetos pertencentes à unidade atual")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Projetos listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping
	public ResponseEntity<List<ProjetoResponseDTO>> listarTodos() {
		return ResponseEntity.ok(projetoService.listarTodos());
	}

	@Operation(summary = "Buscar projeto por ID", description = "Retorna um projeto da unidade atual pelo seu identificador público UUID.")
	@ApiResponses({ @ApiResponse(responseCode = "200", description = "Projeto encontrado", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/{id}")
	public ResponseEntity<ProjetoResponseDTO> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(projetoService.buscarPorId(id));
	}

	@Operation(summary = "Listar projetos por laboratório", description = "Retorna os projetos associados ao laboratório informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Projetos listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-laboratorio")
	public ResponseEntity<List<ProjetoResponseDTO>> listarPorLaboratorio(@RequestParam UUID laboratorioId) {
		return ResponseEntity.ok(projetoService.listarPorLaboratorio(laboratorioId));
	}

	@Operation(summary = "Atualizar projeto", description = "Atualiza os dados do projeto da unidade atual identificado pelo UUID informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "Projeto ou laboratório não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PutMapping("/{id}")
	public ResponseEntity<ProjetoResponseDTO> atualizar(@PathVariable UUID id,
			@Valid @RequestBody ProjetoRequestDTO dto) {
		return ResponseEntity.ok(projetoService.atualizar(id, dto));
	}

	@Operation(summary = "Desativar projeto", description = "Desativa logicamente o projeto identificado pelo UUID informado, preservando seu registro e histórico.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Projeto desativado com sucesso"),
			@ApiResponse(responseCode = "404", description = "Projeto não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {
		projetoService.deletar(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Listar projetos ativos", description = "Retorna os projetos da unidade atual cujo indicador técnico ativo esteja habilitado. Este filtro é independente do status de negócio do projeto.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Projetos ativos listados com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/ativos")
	public ResponseEntity<List<ProjetoResponseDTO>> listarAtivos() {
		return ResponseEntity.ok(projetoService.listarAtivos());
	}
}
