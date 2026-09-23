package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.LocalArmazenamentoResiduoRequestDTO;
import com.sgl.dto.response.LocalArmazenamentoResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.LocalArmazenamentoResiduo;
import com.sgl.model.Unidade;
import com.sgl.repository.LocalArmazenamentoResiduoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocalArmazenamentoResiduoService {

	private final LocalArmazenamentoResiduoRepository localArmazenamentoResiduoRepository;

	private final UnidadeRepository unidadeRepository;

	@Transactional
	public LocalArmazenamentoResiduoResponseDTO criar(LocalArmazenamentoResiduoRequestDTO dto) {

		exigirTenantAtivo();

		Unidade unidade = buscarUnidade(dto.getUnidadeId());

		validarTenantUnidade(unidade.getPublicId());

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(unidade, nome, null);

		LocalArmazenamentoResiduo local = LocalArmazenamentoResiduo.builder().unidade(unidade).nome(nome)
				.ativo(dto.getAtivo() != null ? dto.getAtivo() : true).build();

		return new LocalArmazenamentoResiduoResponseDTO(localArmazenamentoResiduoRepository.save(local));
	}

	@Transactional(readOnly = true)
	public List<LocalArmazenamentoResiduoResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return localArmazenamentoResiduoRepository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream()
				.map(LocalArmazenamentoResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<LocalArmazenamentoResiduoResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return localArmazenamentoResiduoRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
				.map(LocalArmazenamentoResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public LocalArmazenamentoResiduoResponseDTO buscarPorId(UUID id) {

		return new LocalArmazenamentoResiduoResponseDTO(buscarLocalNoTenant(id));
	}

	@Transactional
	public LocalArmazenamentoResiduoResponseDTO atualizar(UUID id, LocalArmazenamentoResiduoRequestDTO dto) {

		LocalArmazenamentoResiduo local = buscarLocalNoTenant(id);

		validarTenantUnidade(local.getUnidade().getPublicId());

		if (!local.getUnidade().getPublicId().equals(dto.getUnidadeId())) {

			throw new BusinessRuleException("O local de armazenamento não pode ser transferido para outra unidade.");
		}

		String nome = normalizarNome(dto.getNome());

		validarNomeDuplicado(local.getUnidade(), nome, local.getId());

		local.setNome(nome);

		if (dto.getAtivo() != null) {
			local.setAtivo(dto.getAtivo());
		}

		return new LocalArmazenamentoResiduoResponseDTO(localArmazenamentoResiduoRepository.save(local));
	}

	@Transactional
	public void deletar(UUID id) {

		LocalArmazenamentoResiduo local = buscarLocalNoTenant(id);

		local.setAtivo(false);
	}

	private LocalArmazenamentoResiduo buscarLocalNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return localArmazenamentoResiduoRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Local de armazenamento de resíduo", id));
	}

	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
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

	private String normalizarNome(String nome) {
		return nome.trim();
	}

	private void validarNomeDuplicado(Unidade unidade, String nome, Long idAtual) {

		boolean duplicado;

		if (idAtual == null) {

			duplicado = localArmazenamentoResiduoRepository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome);

		} else {

			duplicado = localArmazenamentoResiduoRepository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidade.getId(),
					nome, idAtual);
		}

		if (duplicado) {
			throw new BusinessRuleException("Já existe um local de armazenamento com este nome na unidade.");
		}
	}
}