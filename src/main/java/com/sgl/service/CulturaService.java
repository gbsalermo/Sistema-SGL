package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.CulturaRequestDTO;
import com.sgl.dto.response.CulturaResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Cultura;
import com.sgl.model.Unidade;
import com.sgl.repository.CulturaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CulturaService {

	private final CulturaRepository culturaRepository;
	private final UnidadeRepository unidadeRepository;

	@Transactional
	public CulturaResponseDTO criar(CulturaRequestDTO dto) {

		exigirTenantAtivo();

		Unidade unidade = buscarUnidade(dto.getUnidadeId());

		validarTenantUnidade(unidade.getPublicId());

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(unidade, nome, null);

		Cultura cultura = Cultura.builder().unidade(unidade).nome(nome)
				.ativo(dto.getAtivo() != null ? dto.getAtivo() : true).build();

		return new CulturaResponseDTO(culturaRepository.save(cultura));
	}

	@Transactional(readOnly = true)
	public List<CulturaResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return culturaRepository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream().map(CulturaResponseDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CulturaResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return culturaRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
				.map(CulturaResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public CulturaResponseDTO buscarPorId(UUID id) {

		return new CulturaResponseDTO(buscarCulturaNoTenant(id));
	}

	@Transactional
	public CulturaResponseDTO atualizar(UUID id, CulturaRequestDTO dto) {

		Cultura cultura = buscarCulturaNoTenant(id);

		validarTenantUnidade(cultura.getUnidade().getPublicId());

		if (!cultura.getUnidade().getPublicId().equals(dto.getUnidadeId())) {

			throw new BusinessRuleException("A cultura não pode ser transferida para outra unidade.");
		}

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(cultura.getUnidade(), nome, cultura.getId());

		cultura.setNome(nome);

		if (dto.getAtivo() != null) {
			cultura.setAtivo(dto.getAtivo());
		}

		return new CulturaResponseDTO(culturaRepository.save(cultura));
	}

	@Transactional
	public void deletar(UUID id) {

		Cultura cultura = buscarCulturaNoTenant(id);

		/*
		 * A Cultura pode estar associada a participações históricas.
		 */
		cultura.setAtivo(false);
	}

	private Cultura buscarCulturaNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return culturaRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Cultura", id));
	}

	private Unidade buscarUnidade(UUID unidadeId) {

		return unidadeRepository.findByPublicId(unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
	}

	private void validarTenantUnidade(UUID unidadeId) {

		exigirTenantAtivo();

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

			duplicado = culturaRepository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome);

		} else {

			duplicado = culturaRepository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidade.getId(), nome, idAtual);
		}

		if (duplicado) {

			throw new BusinessRuleException("Já existe uma cultura com este nome na unidade.");
		}
	}
}