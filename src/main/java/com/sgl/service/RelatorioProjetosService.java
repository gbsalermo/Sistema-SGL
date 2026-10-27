
package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.RelatorioProjetosResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Projeto;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioProjetosService {

	private final ProjetoRepository projetoRepository;
	private final SciRepository sciRepository;
	private final AtividadeRepository atividadeRepository;
	private final VinculoEstagioAtividadeRepository participacaoRepository;

	@Transactional(readOnly = true)
	public RelatorioProjetosResponseDTO gerar(UUID laboratorioId, StatusProjeto status,
			SituacaoExecucaoProjeto situacaoExecucao, Boolean ativo, Boolean recursoExterno, LocalDate dataInicio,
			LocalDate dataFim) {

		UUID unidadeId = TenantContext.unidadeAtual()
				.orElseThrow(() -> new BusinessRuleException("A Unidade é obrigatória para gerar o relatório."));

		validarPeriodo(dataInicio, dataFim);

		List<Projeto> projetos = projetoRepository.buscarParaRelatorio(unidadeId, laboratorioId, status,
				situacaoExecucao, ativo, recursoExterno, dataInicio, dataFim);

		if (projetos.isEmpty()) {
			return new RelatorioProjetosResponseDTO(LocalDateTime.now(), 0, 0, 0, 0, 0, List.of());
		}

		List<UUID> projetoIds = projetos.stream().map(Projeto::getPublicId).toList();

		Map<UUID, Long> scisPorProjeto = sciRepository.contarScisPorProjetos(unidadeId, projetoIds).stream()
				.collect(Collectors.toMap(SciRepository.TotalScisPorProjeto::getProjetoId,
						SciRepository.TotalScisPorProjeto::getTotalScis));

		Map<UUID, Long> atividadesPorProjeto = atividadeRepository.contarAtividadesPorProjetos(unidadeId, projetoIds)
				.stream().collect(Collectors.toMap(AtividadeRepository.TotalAtividadesPorProjeto::getProjetoId,
						AtividadeRepository.TotalAtividadesPorProjeto::getTotalAtividades));

		var participacoes = participacaoRepository.contarParticipacoesPorProjetos(unidadeId, projetoIds);

		Map<UUID, VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto> participacoesPorProjeto = participacoes
				.stream().collect(Collectors.toMap(
						VinculoEstagioAtividadeRepository.ParticipacoesPorProjeto::getProjetoId, Function.identity()));

		long totalEstagiariosDistintos = participacaoRepository.contarEstagiariosDistintosNosProjetos(unidadeId,
				projetoIds);

		List<RelatorioProjetosResponseDTO.ProjetoItem> itens = projetos.stream().map(projeto -> {

			UUID id = projeto.getPublicId();

			var participacao = participacoesPorProjeto.get(id);

			long totalParticipacoes = participacao == null ? 0L : participacao.getTotalParticipacoes();

			long totalEstagiarios = participacao == null ? 0L : participacao.getTotalEstagiarios();

			return new RelatorioProjetosResponseDTO.ProjetoItem(id, projeto.getCodigoSeg(), projeto.getNome(),
					projeto.getLaboratorio().getPublicId(), projeto.getLaboratorio().getNome(),
					projeto.getResponsavel(), projeto.getDataInicio(), projeto.getDataFim(), projeto.getStatus(),
					projeto.getSituacaoExecucao(), projeto.getAtivo(), projeto.getPossuiRecursoExterno(),
					projeto.getEmpresaRecursoExterno(), scisPorProjeto.getOrDefault(id, 0L),
					atividadesPorProjeto.getOrDefault(id, 0L), totalParticipacoes, totalEstagiarios);

		}).toList();

		long totalScis = itens.stream().mapToLong(RelatorioProjetosResponseDTO.ProjetoItem::totalScis).sum();

		long totalAtividades = itens.stream().mapToLong(RelatorioProjetosResponseDTO.ProjetoItem::totalAtividades)
				.sum();

		long totalParticipacoes = itens.stream().mapToLong(RelatorioProjetosResponseDTO.ProjetoItem::totalParticipacoes)
				.sum();

		return new RelatorioProjetosResponseDTO(LocalDateTime.now(), itens.size(), totalScis, totalAtividades,
				totalParticipacoes, totalEstagiariosDistintos, itens);
	}

	private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {

		if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {

			throw new BusinessRuleException("A data inicial não pode ser posterior à data final.");
		}
	}
}
