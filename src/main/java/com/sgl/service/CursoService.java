package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.CursoRequestDTO;
import com.sgl.dto.response.CursoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Curso;
import com.sgl.model.Unidade;
import com.sgl.repository.CursoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CursoService {

	private final CursoRepository cursoRepository;
	private final UnidadeRepository unidadeRepository;

	@Transactional
	public CursoResponseDTO criar(CursoRequestDTO dto) {

		Unidade unidade = buscarUnidade(dto.getUnidadeId());

		validarTenantUnidade(unidade.getPublicId());

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(unidade, nome, null);

		Curso curso = Curso.builder().unidade(unidade).nome(nome).ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
				.build();

		return new CursoResponseDTO(cursoRepository.save(curso));
	}

	@Transactional(readOnly = true)
	public List<CursoResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return cursoRepository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream().map(CursoResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CursoResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return cursoRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
				.map(CursoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public CursoResponseDTO buscarPorId(UUID id) {

		return new CursoResponseDTO(buscarCursoNoTenant(id));
	}

	@Transactional
	public CursoResponseDTO atualizar(UUID id, CursoRequestDTO dto) {

		Curso curso = buscarCursoNoTenant(id);

		validarTenantUnidade(curso.getUnidade().getPublicId());

		/*
		 * Curso pertence à Unidade em que foi criado. Não permitimos transferi-lo
		 * posteriormente.
		 */
		if (!curso.getUnidade().getPublicId().equals(dto.getUnidadeId())) {

			throw new BusinessRuleException("O curso não pode ser transferido para outra unidade.");
		}

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(curso.getUnidade(), nome, curso.getId());

		curso.setNome(nome);

		if (dto.getAtivo() != null) {
			curso.setAtivo(dto.getAtivo());
		}

		return new CursoResponseDTO(cursoRepository.save(curso));
	}

	@Transactional
	public void deletar(UUID id) {

		Curso curso = buscarCursoNoTenant(id);

		/*
		 * Não excluímos fisicamente porque vínculos históricos podem continuar
		 * apontando para este curso.
		 */
		curso.setAtivo(false);
	}

	private Curso buscarCursoNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return cursoRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Curso", id));
	}

	private Unidade buscarUnidade(UUID unidadeId) {

		return unidadeRepository.findByPublicId(unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
	}

	private void validarTenantUnidade(UUID unidadeId) {

		if (!TenantContext.pertence(unidadeId)) {

			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private String normalizarNome(String nome) {

		return nome.trim();
	}

	private void validarNomeDuplicado(Unidade unidade, String nome, Long idAtual) {

		boolean duplicado;

		if (idAtual == null) {

			duplicado = cursoRepository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome);

		} else {

			duplicado = cursoRepository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidade.getId(), nome, idAtual);
		}

		if (duplicado) {

			throw new BusinessRuleException("Já existe um curso com este nome na unidade.");
		}
	}
}