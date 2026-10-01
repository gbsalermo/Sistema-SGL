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

import com.sgl.dto.request.CulturaRequestDTO;
import com.sgl.dto.response.CulturaResponseDTO;
import com.sgl.service.CulturaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Culturas", description = "Cadastro de culturas de pesquisa disponíveis por Unidade.")
@RestController
@RequestMapping("/api/v1/culturas")
@RequiredArgsConstructor
public class CulturaController {

	private final CulturaService culturaService;

	@Operation(summary = "Criar cultura")
	@PostMapping
	public ResponseEntity<CulturaResponseDTO> criar(@Valid @RequestBody CulturaRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(culturaService.criar(dto));
	}

	@Operation(summary = "Listar culturas")
	@GetMapping
	public ResponseEntity<List<CulturaResponseDTO>> listarTodos() {

		return ResponseEntity.ok(culturaService.listarTodos());
	}

	@Operation(summary = "Listar culturas ativas")
	@GetMapping("/ativos")
	public ResponseEntity<List<CulturaResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(culturaService.listarAtivos());
	}

	@Operation(summary = "Buscar cultura por ID")
	@GetMapping("/{id}")
	public ResponseEntity<CulturaResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(culturaService.buscarPorId(id));
	}

	@Operation(summary = "Atualizar cultura")
	@PutMapping("/{id}")
	public ResponseEntity<CulturaResponseDTO> atualizar(@PathVariable UUID id,
			@Valid @RequestBody CulturaRequestDTO dto) {

		return ResponseEntity.ok(culturaService.atualizar(id, dto));
	}

	@Operation(summary = "Inativar cultura", description = "Inativa a cultura preservando participações históricas.")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		culturaService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}