package com.sgl.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EncerrarVinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeCulturasRequestDTO;
import com.sgl.dto.request.VinculoEstagioAtividadeRequestDTO;
import com.sgl.dto.response.VinculoEstagioAtividadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Cultura;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.VinculoEstagioAtividadeCultura;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.CulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeCulturaRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VinculoEstagioAtividadeService {

	private final VinculoEstagioAtividadeRepository participacaoRepository;
	private final VinculoEstagioRepository vinculoEstagioRepository;
	private final AtividadeRepository atividadeRepository;
	private final CulturaRepository culturaRepository;
	private final VinculoEstagioAtividadeCulturaRepository participacaoCulturaRepository;

	@Transactional
	public VinculoEstagioAtividadeResponseDTO adicionar(UUID vinculoId, VinculoEstagioAtividadeRequestDTO dto) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		validarVinculoOperacional(vinculo);

		Atividade atividade = buscarAtividadeNoTenant(dto.getAtividadeId());

		validarAtividadeOperacional(atividade);

		validarMesmaUnidade(vinculo, atividade);

		validarPeriodo(vinculo, atividade, dto.getDataInicioParticipacao(), null);

		boolean jaPossuiParticipacaoAberta = participacaoRepository
				.existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(vinculo.getId(), atividade.getId());

		if (jaPossuiParticipacaoAberta) {
			throw new BusinessRuleException("O vínculo de estágio já possui participação ativa nesta Atividade.");
		}

		VinculoEstagioAtividade participacao = new VinculoEstagioAtividade();

		participacao.setVinculoEstagio(vinculo);
		participacao.setAtividade(atividade);

		participacao.setDataInicioParticipacao(dto.getDataInicioParticipacao());

		participacao.setObservacao(normalizarObservacao(dto.getObservacao()));

		participacao = participacaoRepository.save(participacao);

		sincronizarCulturas(participacao, dto.getCulturaIds());

		return montarResponse(participacao);
	}

	@Transactional(readOnly = true)
	public List<VinculoEstagioAtividadeResponseDTO> listarPorVinculo(UUID vinculoId) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
						vinculo.getPublicId(), unidadeId)
				.stream().map(this::montarResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<VinculoEstagioAtividadeResponseDTO> listarAtivasPorVinculo(UUID vinculoId) {

		VinculoEstagio vinculo = buscarVinculoNoTenant(vinculoId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return participacaoRepository
				.findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdAndDataFimParticipacaoIsNull(
						vinculo.getPublicId(), unidadeId)
				.stream().map(this::montarResponse).toList();
	}

	@Transactional
	public VinculoEstagioAtividadeResponseDTO encerrar(UUID participacaoId,
			EncerrarVinculoEstagioAtividadeRequestDTO dto) {

		VinculoEstagioAtividade participacao = buscarParticipacaoNoTenant(participacaoId);

		// 1. Não pode encerrar novamente algo que já terminou
		if (participacao.getDataFimParticipacao() != null) {
			throw new BusinessRuleException("A participação nesta Atividade já está encerrada.");
		}

		// 2. Descobrimos qual vínculo de estágio é dono dessa participação
		VinculoEstagio vinculo = participacao.getVinculoEstagio();

		validarVinculoOperacional(vinculo);

		// 3. Conta quantas Atividades ainda estão abertas nesse vínculo
		long participacoesAtivas = participacaoRepository
				.countByVinculoEstagioIdAndDataFimParticipacaoIsNull(vinculo.getId());

		// 4. Se essa for a única, não podemos encerrá-la
		if (participacoesAtivas <= 1) {
			throw new BusinessRuleException("Não é possível encerrar a última participação ativa "
					+ "enquanto o vínculo de estágio estiver em andamento.");
		}

		// 5. Valida se a data de encerramento é válida
		validarPeriodo(vinculo, participacao.getAtividade(), participacao.getDataInicioParticipacao(),
				dto.getDataFimParticipacao());

		// 6. Finalmente encerra a participação
		participacao.setDataFimParticipacao(dto.getDataFimParticipacao());

		participacao = participacaoRepository.save(participacao);

		return montarResponse(participacao);
	}

	private VinculoEstagio buscarVinculoNoTenant(UUID vinculoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return vinculoEstagioRepository.findByPublicIdAndEstagiarioUnidadePublicId(vinculoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Vínculo de estágio", vinculoId));
	}

	private Atividade buscarAtividadeNoTenant(UUID atividadeId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(atividadeId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Atividade", atividadeId));
	}

	private VinculoEstagioAtividade buscarParticipacaoNoTenant(UUID participacaoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return participacaoRepository
				.findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(participacaoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Participação em Atividade", participacaoId));
	}

	private void validarVinculoOperacional(VinculoEstagio vinculo) {

		if (!Boolean.TRUE.equals(vinculo.getEstagiario().getAtivo())) {

			throw new BusinessRuleException("O usuário do vínculo de estágio está inativo.");
		}

		if (vinculo.getSituacao() == SituacaoEstagio.FINALIZADO) {

			throw new BusinessRuleException("Não é possível alterar Atividades de um vínculo de estágio finalizado.");
		}
	}

	private void validarAtividadeOperacional(Atividade atividade) {

		atividade.validateActive();

		Sci sci = atividade.getSci();

		if (sci == null) {
			throw new BusinessRuleException("A Atividade informada não possui SCI válido.");
		}

		sci.validateActive();

		Projeto projeto = sci.getProjeto();

		if (projeto == null) {
			throw new BusinessRuleException("A Atividade informada não possui Projeto válido.");
		}

		projeto.validateActive();

		Laboratorio laboratorio = projeto.getLaboratorio();

		if (laboratorio == null) {
			throw new BusinessRuleException("A Atividade informada não possui Laboratório válido.");
		}

		laboratorio.validateActive();
	}

	private void validarMesmaUnidade(VinculoEstagio vinculo, Atividade atividade) {

		UUID unidadeEstagiario = vinculo.getEstagiario().getUnidade().getPublicId();

		UUID unidadeAtividade = atividade.getSci().getProjeto().getLaboratorio().getUnidade().getPublicId();

		if (!unidadeEstagiario.equals(unidadeAtividade)) {

			throw new BusinessRuleException("O Estagiário e a Atividade devem pertencer à mesma Unidade.");
		}
	}

	private void validarPeriodo(VinculoEstagio vinculo, Atividade atividade, LocalDate inicio, LocalDate fim) {

		if (inicio == null) {
			throw new BusinessRuleException("Data de início da participação é obrigatória.");
		}

		if (fim != null && fim.isBefore(inicio)) {
			throw new BusinessRuleException("Data de fim da participação não pode ser anterior à data de início.");
		}

		if (inicio.isBefore(vinculo.getDataInicio())) {

			throw new BusinessRuleException("A participação não pode começar antes do vínculo de estágio.");
		}

		if (vinculo.getDataFimPrevista() != null && inicio.isAfter(vinculo.getDataFimPrevista())) {

			throw new BusinessRuleException(
					"A participação não pode começar após o fim previsto do vínculo de estágio.");
		}

		if (atividade.getDataInicio() != null && inicio.isBefore(atividade.getDataInicio())) {

			throw new BusinessRuleException("A participação não pode começar antes da Atividade.");
		}

		if (atividade.getDataFim() != null && inicio.isAfter(atividade.getDataFim())) {

			throw new BusinessRuleException("A participação não pode começar após o fim da Atividade.");
		}

		if (fim != null) {

			if (vinculo.getDataFimPrevista() != null && fim.isAfter(vinculo.getDataFimPrevista())) {

				throw new BusinessRuleException(
						"A participação não pode terminar após o fim previsto do vínculo de estágio.");
			}

			if (atividade.getDataFim() != null && fim.isAfter(atividade.getDataFim())) {

				throw new BusinessRuleException("A participação não pode terminar após o fim da Atividade.");
			}
		}
	}

	private String normalizarObservacao(String observacao) {

		if (observacao == null || observacao.isBlank()) {
			return null;
		}

		return observacao.trim();
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	@Transactional
	public VinculoEstagioAtividadeResponseDTO atualizarCulturas(UUID participacaoId,
			VinculoEstagioAtividadeCulturasRequestDTO dto) {

		VinculoEstagioAtividade participacao = buscarParticipacaoNoTenant(participacaoId);

		if (participacao.getDataFimParticipacao() != null) {

			throw new BusinessRuleException("Não é possível alterar Culturas de uma participação encerrada.");
		}

		validarVinculoOperacional(participacao.getVinculoEstagio());

		sincronizarCulturas(participacao, dto.getCulturaIds());

		return montarResponse(participacao);
	}

	private void sincronizarCulturas(VinculoEstagioAtividade participacao, Set<UUID> culturaIds) {

		Set<UUID> ids = culturaIds != null ? new LinkedHashSet<>(culturaIds) : new LinkedHashSet<>();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<Cultura> culturas = ids.isEmpty() ? List.of()
				: culturaRepository.findByPublicIdInAndUnidadePublicId(ids, unidadeId);

		if (culturas.size() != ids.size()) {

			throw new BusinessRuleException("Uma ou mais culturas são inválidas para esta unidade.");
		}

		List<VinculoEstagioAtividadeCultura> atuais = participacaoCulturaRepository
				.findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
						participacao.getPublicId(), unidadeId);

		Map<UUID, VinculoEstagioAtividadeCultura> atuaisPorCultura = new LinkedHashMap<>();

		for (VinculoEstagioAtividadeCultura atual : atuais) {

			atuaisPorCultura.put(atual.getCultura().getPublicId(), atual);
		}

		for (Cultura cultura : culturas) {

			if (!atuaisPorCultura.containsKey(cultura.getPublicId())) {

				cultura.validateActive();
			}
		}

		List<VinculoEstagioAtividadeCultura> remover = atuais.stream()
				.filter(associacao -> !ids.contains(associacao.getCultura().getPublicId())).toList();

		if (!remover.isEmpty()) {

			participacaoCulturaRepository.deleteAll(remover);
		}

		List<VinculoEstagioAtividadeCultura> adicionar = culturas.stream()
				.filter(cultura -> !atuaisPorCultura.containsKey(cultura.getPublicId())).map(cultura -> {

					VinculoEstagioAtividadeCultura associacao = new VinculoEstagioAtividadeCultura();

					associacao.setParticipacao(participacao);
					associacao.setCultura(cultura);

					return associacao;
				}).toList();

		if (!adicionar.isEmpty()) {

			participacaoCulturaRepository.saveAll(adicionar);
		}
	}

	private VinculoEstagioAtividadeResponseDTO montarResponse(VinculoEstagioAtividade participacao) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<VinculoEstagioAtividadeCultura> culturas = participacaoCulturaRepository
				.findByParticipacaoPublicIdAndParticipacaoVinculoEstagioEstagiarioUnidadePublicIdOrderByCulturaNomeAsc(
						participacao.getPublicId(), unidadeId);

		return new VinculoEstagioAtividadeResponseDTO(participacao, culturas);
	}
}