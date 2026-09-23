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

import com.sgl.dto.request.LocalArmazenamentoResiduoRequestDTO;
import com.sgl.dto.response.LocalArmazenamentoResiduoResponseDTO;
import com.sgl.service.LocalArmazenamentoResiduoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/locais-armazenamento-residuo")
@RequiredArgsConstructor
public class LocalArmazenamentoResiduoController {

	private final LocalArmazenamentoResiduoService localArmazenamentoResiduoService;

	@PostMapping
	public ResponseEntity<LocalArmazenamentoResiduoResponseDTO> criar(
			@Valid @RequestBody LocalArmazenamentoResiduoRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(localArmazenamentoResiduoService.criar(dto));
	}

	@GetMapping
	public ResponseEntity<List<LocalArmazenamentoResiduoResponseDTO>> listarTodos() {

		return ResponseEntity.ok(localArmazenamentoResiduoService.listarTodos());
	}

	@GetMapping("/ativos")
	public ResponseEntity<List<LocalArmazenamentoResiduoResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(localArmazenamentoResiduoService.listarAtivos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<LocalArmazenamentoResiduoResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(localArmazenamentoResiduoService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<LocalArmazenamentoResiduoResponseDTO> atualizar(@PathVariable UUID id,
			@Valid @RequestBody LocalArmazenamentoResiduoRequestDTO dto) {

		return ResponseEntity.ok(localArmazenamentoResiduoService.atualizar(id, dto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		localArmazenamentoResiduoService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}