package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.SciRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjetoService {

	private final ProjetoRepository projetoRepository;
	private final LaboratorioRepository laboratorioRepository;
	private final SciRepository sciRepository;
	private final AtividadeRepository atividadeRepository;
	private final CodigoSegValidator codigoSegValidator;

	@Transactional
	public ProjetoResponseDTO criar(ProjetoRequestDTO dto) {
		Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

		Projeto projeto = Projeto.builder().laboratorio(laboratorio).build();
		preencherProjeto(projeto, dto);

		Projeto salvo = projetoRepository.save(projeto);
		return new ProjetoResponseDTO(salvo);
	}

	@Transactional(readOnly = true)
	public List<ProjetoResponseDTO> listarTodos() {
		// Correção de segurança: sem tenant ativo, caía num "findAll" que
		// devolvia projetos de todas as unidades. Agora o header
		// X-SGL-Unidade-Id é exigido também para listar.
		exigirTenantAtivo();

		List<Projeto> projetos = projetoRepository
				.findByLaboratorioUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());
		return projetos.stream().map(ProjetoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public ProjetoResponseDTO buscarPorId(UUID id) {
		return new ProjetoResponseDTO(buscarProjetoNoTenant(id));
	}

	@Transactional(readOnly = true)
	public List<ProjetoResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
		Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
		validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

		return projetoRepository.findByLaboratorioId(laboratorio.getId()).stream().map(ProjetoResponseDTO::new)
				.toList();
	}

	@Transactional
	public ProjetoResponseDTO atualizar(UUID id, ProjetoRequestDTO dto) {
		Projeto projeto = buscarProjetoNoTenant(id);
		Laboratorio novoLaboratorio = buscarLaboratorio(dto.getLaboratorioId());
		validarTenantUnidade(novoLaboratorio.getUnidade() != null ? novoLaboratorio.getUnidade().getPublicId() : null);

		validarDataInicioImutavel(projeto.getDataInicio(), dto.getDataInicio());
		validarAlteracaoDataFim(projeto.getDataFim(), dto.getDataFim());
		validarPeriodoComDescendentes(projeto, dto.getDataFim());

		projeto.setLaboratorio(novoLaboratorio);
		preencherProjeto(projeto, dto);
		return new ProjetoResponseDTO(projetoRepository.save(projeto));
	}

	@Transactional
	public void deletar(UUID id) {
		Projeto projeto = buscarProjetoNoTenant(id);
		projeto.setAtivo(false);
	}

	@Transactional(readOnly = true)
	public List<ProjetoResponseDTO> listarAtivos() {
		exigirTenantAtivo();

		List<Projeto> projetos = projetoRepository
				.findByLaboratorioUnidadePublicIdAndAtivoTrue(TenantContext.unidadeAtual().orElseThrow());
		return projetos.stream().map(ProjetoResponseDTO::new).toList();
	}

	private Projeto buscarProjetoNoTenant(UUID id) {
		// Correção de segurança: antes, sem tenant ativo, buscava sem
		// filtro de unidade (findByPublicId), vazando o projeto de outra
		// unidade para quem não enviasse o header.
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
	}

	private void preencherProjeto(Projeto projeto, ProjetoRequestDTO dto) {
		projeto.setNome(dto.getNome());
		projeto.setDescricao(dto.getDescricao());
		projeto.updateDates(dto.getDataInicio(), dto.getDataFim());
		projeto.setResponsavel(dto.getResponsavel());

		if (projeto.getId() == null) {
			preencherNovosCamposNaCriacao(projeto, dto);
			projeto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
		} else {
			preencherNovosCamposNaAtualizacao(projeto, dto);

			if (dto.getAtivo() != null) {
				projeto.setAtivo(dto.getAtivo());
			}
		}
	}

	private void validarDataInicioImutavel(LocalDate dataInicioAtual, LocalDate dataInicioInformada) {

		if (!Objects.equals(dataInicioAtual, dataInicioInformada)) {
			throw new BusinessRuleException("A data de início do projeto não pode ser alterada após a criação.");
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

	private void validarPeriodoComDescendentes(Projeto projeto, LocalDate novaDataFim) {

		if (novaDataFim == null) {
			return;
		}

		exigirTenantAtivo();
		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		List<Sci> scis = sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(projeto.getPublicId(),
				unidadeId);

		for (Sci sci : scis) {
			if (sci.getDataInicio().isAfter(novaDataFim)
					|| (sci.getDataFim() != null && sci.getDataFim().isAfter(novaDataFim))) {

				throw new BusinessRuleException(
						"A nova data de fim do projeto deixaria um SCI fora do período do projeto.");
			}
		}

		List<Atividade> atividades = atividadeRepository
				.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(projeto.getPublicId(), unidadeId);

		for (Atividade atividade : atividades) {
			if (atividade.getDataInicio().isAfter(novaDataFim)
					|| (atividade.getDataFim() != null && atividade.getDataFim().isAfter(novaDataFim))) {

				throw new BusinessRuleException(
						"A nova data de fim do projeto deixaria uma Atividade fora do período do projeto.");
			}
		}
	}

	private Laboratorio buscarLaboratorio(UUID laboratorioId) {
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		Laboratorio laboratorio = laboratorioRepository.findByPublicIdAndUnidadePublicId(laboratorioId, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

		laboratorio.validateActive();
		return laboratorio;
	}

	private void validarTenantUnidade(UUID unidadeId) {
		if (!TenantContext.pertence(unidadeId)) {
			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	/**
	 * Garante que existe uma unidade (tenant) definida para a requisição atual. Ver
	 * o mesmo método em EstoqueCentralService para a explicação completa do porquê
	 * essa checagem existe.
	 */
	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private void preencherNovosCamposNaCriacao(Projeto projeto, ProjetoRequestDTO dto) {

		String codigoSeg = codigoSegValidator.validarProjeto(dto.getCodigoSeg());

		validarCodigoSegUnico(codigoSeg, null);

		projeto.setCodigoSeg(codigoSeg);

		projeto.setStatus(dto.getStatus() != null ? dto.getStatus() : StatusProjeto.ATIVO);

		projeto.setSituacaoExecucao(
				dto.getSituacaoExecucao() != null ? dto.getSituacaoExecucao() : SituacaoExecucaoProjeto.NAO_INFORMADO);

		boolean possuiRecursoExterno = Boolean.TRUE.equals(dto.getPossuiRecursoExterno());

		projeto.setPossuiRecursoExterno(possuiRecursoExterno);

		aplicarRecursoExterno(projeto, possuiRecursoExterno, dto.getEmpresaRecursoExterno());
	}

	private void preencherNovosCamposNaAtualizacao(Projeto projeto, ProjetoRequestDTO dto) {

		if (dto.getCodigoSeg() != null) {

			String codigoSeg = codigoSegValidator.validarProjeto(dto.getCodigoSeg());

			validarCodigoSegUnico(codigoSeg, projeto.getPublicId());

			projeto.setCodigoSeg(codigoSeg);
		}

		if (dto.getStatus() != null) {
			projeto.setStatus(dto.getStatus());
		}

		if (dto.getSituacaoExecucao() != null) {
			projeto.setSituacaoExecucao(dto.getSituacaoExecucao());
		}

		if (dto.getPossuiRecursoExterno() != null) {
			boolean possuiRecursoExterno = Boolean.TRUE.equals(dto.getPossuiRecursoExterno());

			projeto.setPossuiRecursoExterno(possuiRecursoExterno);

			aplicarRecursoExterno(projeto, possuiRecursoExterno, dto.getEmpresaRecursoExterno());

			return;
		}

		if (dto.getEmpresaRecursoExterno() != null) {
			if (!Boolean.TRUE.equals(projeto.getPossuiRecursoExterno())) {
				throw new BusinessRuleException("Não é possível informar uma empresa sem recurso externo.");
			}

			projeto.setEmpresaRecursoExterno(validarEmpresaRecursoExterno(dto.getEmpresaRecursoExterno()));
		}
	}

	private void aplicarRecursoExterno(Projeto projeto, boolean possuiRecursoExterno, String empresaRecursoExterno) {

		if (!possuiRecursoExterno) {
			projeto.setEmpresaRecursoExterno(null);
			return;
		}

		projeto.setEmpresaRecursoExterno(validarEmpresaRecursoExterno(empresaRecursoExterno));
	}

	private String validarEmpresaRecursoExterno(String empresa) {
		String empresaNormalizada = normalizarTextoOpcional(empresa);

		if (empresaNormalizada == null) {
			throw new BusinessRuleException("A empresa é obrigatória quando o projeto possui recurso externo.");
		}

		return empresaNormalizada;
	}

	private String normalizarTextoOpcional(String valor) {
		if (valor == null) {
			return null;
		}

		String normalizado = valor.trim();
		return normalizado.isEmpty() ? null : normalizado;
	}

	private void validarCodigoSegUnico(String codigoSeg, UUID projetoAtualId) {

		if (codigoSeg == null) {
			return;
		}

		boolean duplicado = projetoAtualId == null ? projetoRepository.existsByCodigoSeg(codigoSeg)
				: projetoRepository.existsByCodigoSegAndPublicIdNot(codigoSeg, projetoAtualId);

		if (duplicado) {
			throw new BusinessRuleException("Já existe um Projeto com este Código SEG.");
		}
	}
}
