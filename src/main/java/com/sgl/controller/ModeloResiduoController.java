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

import com.sgl.dto.request.ModeloResiduoRequestDTO;
import com.sgl.dto.response.ModeloResiduoResponseDTO;
import com.sgl.service.ModeloResiduoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/modelos-residuo")
@RequiredArgsConstructor
public class ModeloResiduoController {

	private final ModeloResiduoService modeloResiduoService;

	@PostMapping
	public ResponseEntity<ModeloResiduoResponseDTO> criar(@Valid @RequestBody ModeloResiduoRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(modeloResiduoService.criar(dto));
	}

	@GetMapping
	public ResponseEntity<List<ModeloResiduoResponseDTO>> listarTodos() {

		return ResponseEntity.ok(modeloResiduoService.listarTodos());
	}

	@GetMapping("/ativos")
	public ResponseEntity<List<ModeloResiduoResponseDTO>> listarAtivos() {

		return ResponseEntity.ok(modeloResiduoService.listarAtivos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ModeloResiduoResponseDTO> buscarPorId(@PathVariable UUID id) {

		return ResponseEntity.ok(modeloResiduoService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ModeloResiduoResponseDTO> atualizar(@PathVariable UUID id,
			@Valid @RequestBody ModeloResiduoRequestDTO dto) {

		return ResponseEntity.ok(modeloResiduoService.atualizar(id, dto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable UUID id) {

		modeloResiduoService.deletar(id);

		return ResponseEntity.noContent().build();
	}
}