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

import com.sgl.dto.request.CursoRequestDTO;
import com.sgl.dto.response.CursoResponseDTO;
import com.sgl.service.CursoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cursos", description = "Cadastro de cursos disponíveis por Unidade.")
@RestController
@RequestMapping("/api/v1/cursos")
@RequiredArgsConstructor
public class CursoController {

	private final CursoService cursoService;

	@Operation(summary = "Criar curso", description = "Cria um novo curso para a Unidade atual.")
	@PostMapping
	public ResponseEntity<CursoResponseDTO> criar(@Valid @RequestBody CursoRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(cursoService.criar(dto));
	}

	@Operation(summary = "Listar cursos", description = "Lista todos os cursos da Unidade atual, ativos ou inativos.")
	@GetMapping
	public ResponseEntity<List<CursoResponseDTO>> listarTodos() {

		return ResponseEntity.ok(cursoService.listarTodos());
	}

	@Operation(summary = "Listar cursos ativos", description = "Lista somente os cursos ativos disponíveis para novos vínculos.")
	@GetMapping("/ativos")
	public ResponseEntity<List<CursoResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(cursoService.listarAtivos());
	}

	@Operation(summary = "Buscar curso por ID")
	@GetMapping("/{id}")
	public ResponseEntity<CursoResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(cursoService.buscarPorId(id));
	}

	@Operation(summary = "Atualizar curso", description = "Atualiza os dados do curso sem permitir mudança de Unidade.")
	@PutMapping("/{id}")
	public ResponseEntity<CursoResponseDTO> atualizar(@PathVariable UUID id, @Valid @RequestBody CursoRequestDTO dto) {

		return ResponseEntity.ok(cursoService.atualizar(id, dto));
	}

	@Operation(summary = "Inativar curso", description = "Inativa o curso preservando vínculos históricos.")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		cursoService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}