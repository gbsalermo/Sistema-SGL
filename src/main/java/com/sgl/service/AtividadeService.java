package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.AtividadeRequestDTO;
import com.sgl.dto.response.AtividadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.SciRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtividadeService {

	private final AtividadeRepository atividadeRepository;
	private final SciRepository sciRepository;
	private final CodigoSegValidator codigoSegValidator;

	@Transactional
	public AtividadeResponseDTO criar(AtividadeRequestDTO dto) {

		Sci sci = buscarSciNoTenant(dto.getSciId());

		sci.validateActive();

		Projeto projeto = sci.getProjeto();

		if (projeto != null) {
			projeto.validateActive();
		}

		validarPeriodoComSci(sci, dto.getDataInicio(), dto.getDataFim());

		Atividade atividade = Atividade.builder().sci(sci).build();

		preencherNaCriacao(atividade, dto);

		return new AtividadeResponseDTO(atividadeRepository.save(atividade));
	}

	@Transactional(readOnly = true)
	public List<AtividadeResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findBySciProjetoLaboratorioUnidadePublicId(unidadeId).stream()
				.map(AtividadeResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<AtividadeResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findBySciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(unidadeId).stream()
				.map(AtividadeResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public AtividadeResponseDTO buscarPorId(UUID id) {

		return new AtividadeResponseDTO(buscarAtividadeNoTenant(id));
	}

	@Transactional(readOnly = true)
	public List<AtividadeResponseDTO> listarPorSci(UUID sciId) {

		Sci sci = buscarSciNoTenant(sciId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository
				.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(sci.getPublicId(), unidadeId).stream()
				.map(AtividadeResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<AtividadeResponseDTO> listarAtivosPorSci(UUID sciId) {

		Sci sci = buscarSciNoTenant(sciId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository
				.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(sci.getPublicId(), unidadeId)
				.stream().map(AtividadeResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<AtividadeResponseDTO> listarPorProjeto(UUID projetoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(projetoId, unidadeId)
				.stream().map(AtividadeResponseDTO::new).toList();
	}

	@Transactional
	public AtividadeResponseDTO atualizar(UUID id, AtividadeRequestDTO dto) {

		Atividade atividade = buscarAtividadeNoTenant(id);

		validarSciImutavel(atividade, dto.getSciId());

		Sci sci = atividade.getSci();

		validarDataInicioImutavel(atividade.getDataInicio(), dto.getDataInicio());

		validarPeriodoComSci(sci, dto.getDataInicio(), dto.getDataFim());

		validarAlteracaoDataFim(atividade.getDataFim(), dto.getDataFim());

		preencherNaAtualizacao(atividade, dto);

		return new AtividadeResponseDTO(atividadeRepository.save(atividade));
	}

	@Transactional
	public void deletar(UUID id) {

		Atividade atividade = buscarAtividadeNoTenant(id);

		atividade.setAtivo(false);
	}

	private void preencherNaCriacao(Atividade atividade, AtividadeRequestDTO dto) {

		atividade.setCodigoSeg(
		        codigoSegValidator.validarAtividade(
		                dto.getCodigoSeg(),
		                atividade.getSci()
		        )
		);

		atividade.setNome(normalizarTextoObrigatorio(dto.getNome(), "O nome da Atividade é obrigatório."));

		atividade.setResponsavel(normalizarTextoOpcional(dto.getResponsavel()));

		atividade.updateDates(dto.getDataInicio(), dto.getDataFim());

		atividade.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusProjeto.ATIVO);

		atividade.setSituacaoExecucao(
				dto.getSituacaoExecucao() != null ? dto.getSituacaoExecucao() : SituacaoExecucaoProjeto.NAO_INFORMADO);

		atividade.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
	}

	private void preencherNaAtualizacao(Atividade atividade, AtividadeRequestDTO dto) {

		atividade.setCodigoSeg(
		        codigoSegValidator.validarAtividade(
		                dto.getCodigoSeg(),
		                atividade.getSci()
		        )
		);

		atividade.setNome(normalizarTextoObrigatorio(dto.getNome(), "O nome da Atividade é obrigatório."));

		atividade.setResponsavel(normalizarTextoOpcional(dto.getResponsavel()));

		atividade.updateDates(dto.getDataInicio(), dto.getDataFim());

		if (dto.getStatus() != null) {
			atividade.setStatus(dto.getStatus());
		}

		if (dto.getSituacaoExecucao() != null) {
			atividade.setSituacaoExecucao(dto.getSituacaoExecucao());
		}

		if (dto.getAtivo() != null) {
			atividade.setAtivo(dto.getAtivo());
		}
	}

	private Sci buscarSciNoTenant(UUID sciId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(sciId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("SCI", sciId));
	}

	private Atividade buscarAtividadeNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Atividade", id));
	}

	private void validarSciImutavel(Atividade atividade, UUID sciIdInformado) {

		if (!atividade.getSci().getPublicId().equals(sciIdInformado)) {

			throw new BusinessRuleException("A Atividade não pode ser transferida para outro SCI.");
		}
	}

	private void validarDataInicioImutavel(
			LocalDate dataInicioAtual,
			LocalDate dataInicioInformada) {

		if (!Objects.equals(dataInicioAtual, dataInicioInformada)) {
			throw new BusinessRuleException(
					"A data de início da Atividade não pode ser alterada após a criação."
			);
		}
	}

	private void validarPeriodoComSci(Sci sci, LocalDate dataInicioAtividade, LocalDate dataFimAtividade) {

		if (dataInicioAtividade == null) {

			throw new BusinessRuleException("A data de início da Atividade é obrigatória.");
		}

		if (sci.getDataInicio() == null) {

			throw new BusinessRuleException("O SCI deve possuir data de início antes de receber uma Atividade.");
		}

		if (dataInicioAtividade.isBefore(sci.getDataInicio())) {

			throw new BusinessRuleException(
					"A data de início da Atividade não pode ser anterior à data de início do SCI.");
		}

		if (dataFimAtividade != null && dataFimAtividade.isBefore(dataInicioAtividade)) {

			throw new BusinessRuleException("A data de fim da Atividade não pode ser anterior à data de início.");
		}

		if (sci.getDataFim() != null && dataInicioAtividade.isAfter(sci.getDataFim())) {

			throw new BusinessRuleException(
					"A data de início da Atividade não pode ser posterior à data de fim do SCI.");
		}

		if (sci.getDataFim() != null && dataFimAtividade != null && dataFimAtividade.isAfter(sci.getDataFim())) {

			throw new BusinessRuleException("A data de fim da Atividade não pode ser posterior à data de fim do SCI.");
		}
	}

	private void validarAlteracaoDataFim(LocalDate dataFimAtual, LocalDate novaDataFim) {

		if (dataFimAtual == null) {
			return;
		}

		if (novaDataFim == null) {

			throw new BusinessRuleException(
					"A data de fim existente não pode ser removida pelo fluxo comum de atualização.");
		}

		if (novaDataFim.isAfter(dataFimAtual)) {

			throw new BusinessRuleException("A ampliação da data final deve ser realizada pelo fluxo de prorrogação.");
		}
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private String normalizarTextoObrigatorio(String valor, String mensagem) {

		if (valor == null || valor.isBlank()) {
			throw new BusinessRuleException(mensagem);
		}

		return valor.trim();
	}

	private String normalizarTextoOpcional(String valor) {

		if (valor == null || valor.isBlank()) {
			return null;
		}

		return valor.trim();
	}
}
