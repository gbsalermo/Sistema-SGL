package com.sgl.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.request.AtualizarVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovaBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoEstagioRequestDTO;
import com.sgl.dto.request.NovoVinculoInstitucionalRequestDTO;
import com.sgl.dto.request.ProrrogarBolsaVinculoEstagioRequestDTO;
import com.sgl.dto.request.SincronizacaoVinculoEstagioRequestDTO;
import com.sgl.dto.response.HistoricoSincronizacaoVinculoEstagioResponseDTO;
import com.sgl.dto.response.VinculoEstagioResponseDTO;
import com.sgl.service.SincronizacaoVinculoEstagioService;
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
	private final SincronizacaoVinculoEstagioService sincronizacaoService;

	@Operation(summary = "Criar novo vínculo de estágio", description = "Cria um novo período institucional para um Estagiário existente "
			+ "já associado obrigatoriamente à primeira Atividade.")
	@PostMapping("/estagiarios/{estagiarioId}")
	public ResponseEntity<VinculoEstagioResponseDTO> criar(@PathVariable UUID estagiarioId,
			@Valid @RequestBody NovoVinculoEstagioRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(estagiarioId, dto));
	}

	@Operation(
			summary = "Atualizar dados operacionais do vínculo",
			description = "Permite ao SGL complementar/ajustar Formação, Curso, Orientador e período. "
					+ "O tipo de bolsa não é alterado por este fluxo; uma nova bolsa gera nova ocorrência de vínculo. "
					+ "Quando a integração institucional informar esses mesmos campos posteriormente, "
					+ "a sincronização institucional prevalece.")
	@PutMapping("/{vinculoId}")
	public ResponseEntity<VinculoEstagioResponseDTO> atualizarLocal(
			@PathVariable UUID vinculoId,
			@Valid @RequestBody AtualizarVinculoEstagioRequestDTO dto) {

		return ResponseEntity.ok(service.atualizarLocal(vinculoId, dto));
	}

	@Operation(
			summary = "Prorrogar bolsa atual localmente",
			description = "Fallback operacional enquanto a integração institucional não fornecer a prorrogação. "
					+ "Mantém a mesma ocorrência de vínculo, amplia a data final prevista e registra histórico auditável.")
	@PutMapping("/{vinculoId}/prorrogar-bolsa")
	public ResponseEntity<VinculoEstagioResponseDTO> prorrogarBolsa(
			@PathVariable UUID vinculoId,
			@Valid @RequestBody ProrrogarBolsaVinculoEstagioRequestDTO dto) {

		return ResponseEntity.ok(service.prorrogarBolsaLocal(vinculoId, dto));
	}

	@Operation(
			summary = "Registrar nova bolsa local",
			description = "Alternativa operacional enquanto a integração institucional não fornecer a nova ocorrência. "
					+ "Finaliza a bolsa/vínculo atual, preserva seu histórico e cria uma nova ocorrência vigente para o mesmo Estagiário.")
	@PostMapping("/{vinculoId}/nova-bolsa")
	public ResponseEntity<VinculoEstagioResponseDTO> registrarNovaBolsa(
			@PathVariable UUID vinculoId,
			@Valid @RequestBody NovaBolsaVinculoEstagioRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarNovaBolsaLocal(vinculoId, dto));
	}

	@Operation(summary = "Sincronizar estado institucional do vínculo", description = "Recebe o estado do vínculo informado pelo ambiente institucional, "
			+ "aplicando atualização, prorrogação ou finalização de forma auditável.")
	@PostMapping("/{vinculoId}/sincronizacoes-institucionais")
	public ResponseEntity<HistoricoSincronizacaoVinculoEstagioResponseDTO> sincronizar(@PathVariable UUID vinculoId,
			@Valid @RequestBody SincronizacaoVinculoEstagioRequestDTO dto) {

		return ResponseEntity.ok(sincronizacaoService.sincronizar(vinculoId, dto));
	}

	@Operation(summary = "Criar novo vínculo recebido do ambiente institucional")
	@PostMapping("/estagiarios/{estagiarioId}/institucional")
	public ResponseEntity<VinculoEstagioResponseDTO> criarInstitucional(@PathVariable UUID estagiarioId,
			@Valid @RequestBody NovoVinculoInstitucionalRequestDTO dto) {

		return ResponseEntity.status(HttpStatus.CREATED).body(service.criarInstitucional(estagiarioId, dto));
	}
}