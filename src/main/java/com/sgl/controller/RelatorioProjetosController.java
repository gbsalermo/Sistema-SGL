
package com.sgl.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sgl.dto.response.RelatorioProjetosResponseDTO;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.RelatorioProjetosService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Relatórios", description = "Consultas consolidadas para operação, gestão e fiscalização.")
@RestController
@RequestMapping("/api/v1/relatorios/projetos")
@RequiredArgsConstructor
public class RelatorioProjetosController {

	private final RelatorioProjetosService relatorioProjetosService;

	@Operation(summary = "Gerar relatório consolidado de Projetos", description = """
			Retorna os Projetos da Unidade atual com filtros opcionais,
			totais de SCI, Atividades, participações e Estagiários distintos.
			O período considera a sobreposição com a execução do Projeto.
			Inclui registros históricos ativos e inativos.
			""")
	@GetMapping
	public ResponseEntity<RelatorioProjetosResponseDTO> gerar(@RequestParam(required = false) UUID laboratorioId,
			@RequestParam(required = false) StatusProjeto status,
			@RequestParam(required = false) SituacaoExecucaoProjeto situacaoExecucao,
			@RequestParam(required = false) Boolean ativo, @RequestParam(required = false) Boolean recursoExterno,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

		return ResponseEntity.ok(relatorioProjetosService.gerar(laboratorioId, status, situacaoExecucao, ativo,
				recursoExterno, dataInicio, dataFim));
	}
}
