package com.sgl.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.EncerrarVinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeCulturasRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioAtividadeResponseDTO;
import com.sgl.service.VinculoEstagioAtividadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Vínculos de Estágio - Atividades", description = "Gerenciamento das participações do estagiário em Atividades.")
@RestController
@RequestMapping("/api/v1/vinculos-estagio")
@RequiredArgsConstructor
public class VinculoEstagioAtividadeController {

	private final VinculoEstagioAtividadeService service;

	@Operation(summary = "Adicionar Atividade ao vínculo de estágio")
	@PostMapping("/{vinculoId}/atividades")
	public ResponseEntity<VinculoEstagioAtividadeResponseDTO> adicionar(@PathVariable UUID vinculoId,
			@Valid @RequestBody VinculoEstagioAtividadeRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionar(vinculoId, dto));
	}

	@Operation(summary = "Listar histórico de Atividades do vínculo")
	@GetMapping("/{vinculoId}/atividades")
	public ResponseEntity<List<VinculoEstagioAtividadeResponseDTO>> listar(@PathVariable UUID vinculoId) {

		return ResponseEntity.ok(service.listarPorVinculo(vinculoId));
	}

	@Operation(summary = "Listar Atividades ativas do vínculo")
	@GetMapping("/{vinculoId}/atividades/ativas")
	public ResponseEntity<List<VinculoEstagioAtividadeResponseDTO>> listarAtivas(@PathVariable UUID vinculoId) {

		return ResponseEntity.ok(service.listarAtivasPorVinculo(vinculoId));
	}

	@Operation(summary = "Editar participação em uma Atividade", description = "Permite corrigir a Atividade, a data de início, a observação e as Culturas de uma participação ativa.")
	@PutMapping("/participacoes/{participacaoId}")
	public ResponseEntity<VinculoEstagioAtividadeResponseDTO> atualizar(@PathVariable UUID participacaoId,
			@Valid @RequestBody VinculoEstagioAtividadeRequestDTO dto) {

		return ResponseEntity.ok(service.atualizar(participacaoId, dto));
	}

	@Operation(summary = "Encerrar participação em uma Atividade")
	@PutMapping("/participacoes/{participacaoId}/encerrar")
	public ResponseEntity<VinculoEstagioAtividadeResponseDTO> encerrar(@PathVariable UUID participacaoId,
			@Valid @RequestBody EncerrarVinculoEstagioAtividadeRequestDTO dto) {

		return ResponseEntity.ok(service.encerrar(participacaoId, dto));
	}

	@Operation(summary = "Definir Culturas da participação em uma Atividade")
	@PutMapping("/participacoes/{participacaoId}/culturas")
	public ResponseEntity<VinculoEstagioAtividadeResponseDTO> atualizarCulturas(@PathVariable UUID participacaoId,
			@Valid @RequestBody VinculoEstagioAtividadeCulturasRequestDTO dto) {

		return ResponseEntity.ok(service.atualizarCulturas(participacaoId, dto));
	}
}