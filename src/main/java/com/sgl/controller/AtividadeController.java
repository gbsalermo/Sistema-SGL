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

import com.sgl.dto.request.AtividadeRequestDTO;
import com.sgl.dto.response.AtividadeResponseDTO;
import com.sgl.exception.ApiError;
import com.sgl.service.AtividadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Atividades", description = "Operações de cadastro, consulta e manutenção das Atividades vinculadas aos SCI.")
@RestController
@RequestMapping("/api/v1/atividades")
@RequiredArgsConstructor
public class AtividadeController {

	private final AtividadeService atividadeService;

	@Operation(summary = "Criar Atividade", description = "Cadastra uma nova Atividade vinculada obrigatoriamente a um SCI da unidade atual.")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Atividade criada com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PostMapping
	public ResponseEntity<AtividadeResponseDTO> criar(@Valid @RequestBody AtividadeRequestDTO dto) {

		AtividadeResponseDTO criada = atividadeService.criar(dto);

		return ResponseEntity.status(HttpStatus.CREATED).body(criada);
	}

	@Operation(summary = "Listar Atividades", description = "Retorna as Atividades pertencentes à unidade atual.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividades listadas com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Contexto de unidade não informado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping
	public ResponseEntity<List<AtividadeResponseDTO>> listarTodos() {

		return ResponseEntity.ok(atividadeService.listarTodos());
	}

	@Operation(summary = "Buscar Atividade por ID", description = "Retorna uma Atividade da unidade atual pelo seu identificador público UUID.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividade encontrada", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "Atividade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/{id}")
	public ResponseEntity<AtividadeResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(atividadeService.buscarPorId(id));
	}

	@Operation(summary = "Listar Atividades por SCI", description = "Retorna as Atividades vinculadas ao SCI informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividades listadas com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-sci")
	public ResponseEntity<List<AtividadeResponseDTO>> listarPorSci(@RequestParam UUID sciId) {

		return ResponseEntity.ok(atividadeService.listarPorSci(sciId));
	}

	@Operation(summary = "Listar Atividades por Projeto", description = "Filtra as Atividades pelo Projeto ao qual seus SCI pertencem.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividades filtradas com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-projeto")
	public ResponseEntity<List<AtividadeResponseDTO>> listarPorProjeto(@RequestParam UUID projetoId) {

		return ResponseEntity.ok(atividadeService.listarPorProjeto(projetoId));
	}

	@Operation(summary = "Listar Atividades habilitadas", description = "Retorna as Atividades da unidade atual cujo indicador técnico ativo esteja habilitado. O filtro é independente do status de negócio.")
	@GetMapping("/ativos")
	public ResponseEntity<List<AtividadeResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(atividadeService.listarAtivos());
	}

	@Operation(summary = "Listar Atividades habilitadas por SCI", description = "Retorna as Atividades tecnicamente ativas vinculadas ao SCI informado.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividades habilitadas listadas com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "404", description = "SCI não encontrado", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@GetMapping("/por-sci/ativos")
	public ResponseEntity<List<AtividadeResponseDTO>> listarAtivosPorSci(@RequestParam UUID sciId) {

		return ResponseEntity.ok(atividadeService.listarAtivosPorSci(sciId));
	}

	@Operation(summary = "Atualizar Atividade", description = "Atualiza uma Atividade preservando obrigatoriamente seu SCI original. A ampliação de uma data final existente deve usar o fluxo específico de prorrogação.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Atividade atualizada com sucesso", useReturnTypeSchema = true),
			@ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "404", description = "Atividade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@PutMapping("/{id}")
	public ResponseEntity<AtividadeResponseDTO> atualizar(@PathVariable UUID id,
			@Valid @RequestBody AtividadeRequestDTO dto) {

		return ResponseEntity.ok(atividadeService.atualizar(id, dto));
	}

	@Operation(summary = "Desativar Atividade", description = "Desativa logicamente a Atividade identificada pelo UUID informado, preservando seu registro.")
	@ApiResponses({ @ApiResponse(responseCode = "204", description = "Atividade desativada com sucesso"),
			@ApiResponse(responseCode = "404", description = "Atividade não encontrada", content = @Content(schema = @Schema(implementation = ApiError.class))),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(schema = @Schema(implementation = ApiError.class))) })
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		atividadeService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}