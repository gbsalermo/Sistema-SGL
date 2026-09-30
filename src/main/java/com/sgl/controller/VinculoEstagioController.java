package com.sgl.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.service.VinculoEstagioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Vínculos de Estágio", description = "Gerenciamento dos vínculos institucionais de estágio.")
@RestController
@RequestMapping("/api/v1/vinculos-estagio")
@RequiredArgsConstructor
public class VinculoEstagioController {

	private final VinculoEstagioService service;

	@Operation(summary = "Criar novo vínculo de estágio", description = "Cria um novo período institucional para um Estagiário existente "
			+ "já associado obrigatoriamente à primeira Atividade.")
	@PostMapping("/estagiarios/{estagiarioId}")
	public ResponseEntity<VinculoEstagioResponseDTO> criar(@PathVariable UUID estagiarioId,
			@Valid @RequestBody NovoVinculoEstagioRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(estagiarioId, dto));
	}
}