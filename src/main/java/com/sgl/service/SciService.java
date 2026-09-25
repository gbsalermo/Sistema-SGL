package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.SciRequestDTO;
import com.sgl.dto.response.SciResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SciService {

	private final SciRepository sciRepository;
	private final ProjetoRepository projetoRepository;

	@Transactional
	public SciResponseDTO criar(SciRequestDTO dto) {

		Projeto projeto = buscarProjetoNoTenant(dto.getProjetoId());

		projeto.validateActive();

		validarPeriodoComProjeto(projeto, dto.getDataInicio(), dto.getDataFim());

		Sci sci = Sci.builder().projeto(projeto).build();

		preencherSciNaCriacao(sci, dto);

		return new SciResponseDTO(sciRepository.save(sci));
	}

	@Transactional(readOnly = true)
	public List<SciResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByProjetoLaboratorioUnidadePublicId(unidadeId).stream().map(SciResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<SciResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByProjetoLaboratorioUnidadePublicIdAndAtivoTrue(unidadeId).stream()
				.map(SciResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public SciResponseDTO buscarPorId(UUID id) {

		return new SciResponseDTO(buscarSciNoTenant(id));
	}

	@Transactional(readOnly = true)
	public List<SciResponseDTO> listarPorProjeto(UUID projetoId) {

		Projeto projeto = buscarProjetoNoTenant(projetoId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(projeto.getPublicId(), unidadeId)
				.stream().map(SciResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<SciResponseDTO> listarAtivosPorProjeto(UUID projetoId) {

		Projeto projeto = buscarProjetoNoTenant(projetoId);

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository
				.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicIdAndAtivoTrue(projeto.getPublicId(), unidadeId)
				.stream().map(SciResponseDTO::new).toList();
	}

	@Transactional
	public SciResponseDTO atualizar(UUID id, SciRequestDTO dto) {

		Sci sci = buscarSciNoTenant(id);

		validarProjetoImutavel(sci, dto.getProjetoId());

		Projeto projeto = sci.getProjeto();

		validarPeriodoComProjeto(projeto, dto.getDataInicio(), dto.getDataFim());

		preencherSciNaAtualizacao(sci, dto);

		return new SciResponseDTO(sciRepository.save(sci));
	}

	@Transactional
	public void deletar(UUID id) {

		Sci sci = buscarSciNoTenant(id);

		sci.setAtivo(false);
	}

	private void preencherSciNaCriacao(Sci sci, SciRequestDTO dto) {

		sci.setCodigoSeg(normalizarTextoObrigatorio(dto.getCodigoSeg(), "O código SEG do SCI é obrigatório."));

		sci.setNome(normalizarTextoObrigatorio(dto.getNome(), "O nome do SCI é obrigatório."));

		sci.setResponsavel(normalizarTextoOpcional(dto.getResponsavel()));

		sci.updateDates(dto.getDataInicio(), dto.getDataFim());

		sci.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusProjeto.ATIVO);

		sci.setSituacaoExecucao(
				dto.getSituacaoExecucao() != null ? dto.getSituacaoExecucao() : SituacaoExecucaoProjeto.NAO_INFORMADO);

		sci.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
	}

	private void preencherSciNaAtualizacao(Sci sci, SciRequestDTO dto) {

		sci.setCodigoSeg(normalizarTextoObrigatorio(dto.getCodigoSeg(), "O código SEG do SCI é obrigatório."));

		sci.setNome(normalizarTextoObrigatorio(dto.getNome(), "O nome do SCI é obrigatório."));

		sci.setResponsavel(normalizarTextoOpcional(dto.getResponsavel()));

		sci.updateDates(dto.getDataInicio(), dto.getDataFim());

		if (dto.getStatus() != null) {
			sci.setStatus(dto.getStatus());
		}

		if (dto.getSituacaoExecucao() != null) {
			sci.setSituacaoExecucao(dto.getSituacaoExecucao());
		}

		if (dto.getAtivo() != null) {
			sci.setAtivo(dto.getAtivo());
		}
	}

	private Projeto buscarProjetoNoTenant(UUID projetoId) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(projetoId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));
	}

	private Sci buscarSciNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("SCI", id));
	}

	private void validarProjetoImutavel(Sci sci, UUID projetoIdInformado) {

		if (!sci.getProjeto().getPublicId().equals(projetoIdInformado)) {

			throw new BusinessRuleException("O SCI não pode ser transferido para outro projeto.");
		}
	}

	private void validarPeriodoComProjeto(Projeto projeto, LocalDate dataInicioSci, LocalDate dataFimSci) {

		if (dataInicioSci == null) {
			throw new BusinessRuleException("A data de início do SCI é obrigatória.");
		}

		if (projeto.getDataInicio() == null) {
			throw new BusinessRuleException("O projeto deve possuir data de início antes de receber um SCI.");
		}

		if (dataInicioSci.isBefore(projeto.getDataInicio())) {

			throw new BusinessRuleException(
					"A data de início do SCI não pode ser anterior à data de início do projeto.");
		}

		if (dataFimSci != null && dataFimSci.isBefore(dataInicioSci)) {

			throw new BusinessRuleException("A data de fim do SCI não pode ser anterior à data de início.");
		}

		if (projeto.getDataFim() != null && dataFimSci != null && dataFimSci.isAfter(projeto.getDataFim())) {

			throw new BusinessRuleException("A data de fim do SCI não pode ser posterior à data de fim do projeto.");
		}
		
		if (projeto.getDataFim() != null
		        && dataInicioSci.isAfter(projeto.getDataFim())) {

		    throw new BusinessRuleException(
		            "A data de início do SCI não pode ser posterior à data de fim do projeto.");
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