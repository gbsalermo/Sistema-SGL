package com.sgl.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.EstoqueCentralRequestDTO;
import com.sgl.dto.response.EstoqueCentralResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.RecipienteEstoqueRepository;
import com.sgl.model.RecipienteEstoque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

	private final EstoqueCentralRepository estoqueCentralRepository;
	private final ProdutoRepository produtoRepository;
	private final RecipienteEstoqueRepository recipienteEstoqueRepository;
	private final UnidadeRepository unidadeRepository;

	private void configurarMinimo(EstoqueCentral estoque, EstoqueCentralRequestDTO dto) {
		if (dto.getQuantidadeMinima() == null || dto.getQuantidadeMinima().signum() < 0) {
			throw new BusinessRuleException("A quantidade mínima deve ser maior ou igual a zero.");
		}
		var tipo = dto.getMinimoTipoEmbalagem();
		var fator = dto.getMinimoMultiplicador();
		if ((tipo == null) != (fator == null)) {
			throw new BusinessRuleException("Informe a apresentação e o conteúdo por embalagem juntos.");
		}
		if (tipo != null) {
			if (fator.signum() <= 0 || fator.scale() > 6) {
				throw new BusinessRuleException("O conteúdo por embalagem deve ser positivo e ter até seis casas decimais.");
			}
			var quantidade = dto.getQuantidadeMinima().divideAndRemainder(fator);
			if (quantidade[1].signum() != 0) {
				throw new BusinessRuleException("O mínimo em embalagens deve ser inteiro.");
			}
		}
		estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
		estoque.setMinimoTipoEmbalagem(tipo);
		estoque.setMinimoMultiplicador(fator);
	}

	@Transactional
	public EstoqueCentralResponseDTO criar(EstoqueCentralRequestDTO dto) {
		validarTenantUnidade(dto.getUnidadeId());

		Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
				.orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

		Produto produto = produtoRepository.findByPublicId(dto.getProdutoId())
				.orElseThrow(() -> new ResourceNotFoundException("Produto", dto.getProdutoId()));

		if (estoqueCentralRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())) {
			throw new BusinessRuleException("Já existe estoque para esse produto nesta unidade.");
		}

		if (!Boolean.TRUE.equals(produto.getAtivo())) {
			throw new BusinessRuleException("Não é possível criar estoque para produto inativo.");
		}

		EstoqueCentral estoque = EstoqueCentral.builder().unidade(unidade).produto(produto)
				.quantidadeAtual(BigDecimal.ZERO).quantidadeMinima(dto.getQuantidadeMinima())
				.ativo(dto.getAtivo() != null ? dto.getAtivo() : true).build();

		configurarMinimo(estoque, dto);
		return comResumoFisico(estoqueCentralRepository.save(estoque));
	}

	@Transactional(readOnly = true)
	public List<EstoqueCentralResponseDTO> listarTodos() {
		// Correção de segurança: antes, se a requisição chegasse sem o header
		// X-SGL-Unidade-Id, caíamos no "findAll" e devolvíamos o estoque de
		// TODAS as unidades. Agora exigimos o tenant também para listar.
		exigirTenantAtivo();

		List<EstoqueCentral> estoques = estoqueCentralRepository
				.findByUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());

		return comResumosFisicos(estoques, TenantContext.unidadeAtual().orElseThrow());
	}

	@Transactional(readOnly = true)
	public EstoqueCentralResponseDTO buscarPorId(UUID id) {
		return comResumoFisico(buscarEstoqueNoTenant(id));
	}

	@Transactional(readOnly = true)
	public EstoqueCentralResponseDTO buscarPorUnidadeEProduto(UUID unidadeId, UUID produtoId) {
		validarTenantUnidade(unidadeId);
		Unidade unidade = buscarUnidade(unidadeId);
		Produto produto = produtoRepository.findByPublicId(produtoId)
				.orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

		EstoqueCentral estoque = estoqueCentralRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Estoque da unidade " + unidadeId + " para o produto " + produtoId));
		return comResumoFisico(estoque);
	}

	@Transactional(readOnly = true)
	public List<EstoqueCentralResponseDTO> listarPorUnidade(UUID unidadeId) {
		validarTenantUnidade(unidadeId);
		return comResumosFisicos(estoqueCentralRepository.findByUnidadePublicId(unidadeId), unidadeId);
	}

	@Transactional
	public EstoqueCentralResponseDTO atualizar(UUID id, EstoqueCentralRequestDTO dto) {
		EstoqueCentral estoque = buscarEstoqueNoTenant(id);

		configurarMinimo(estoque, dto);
		if (dto.getAtivo() != null) {
			estoque.setAtivo(dto.getAtivo());
		}

		return comResumoFisico(estoqueCentralRepository.save(estoque));
	}

	@Transactional(readOnly = true)
	public List<EstoqueCentralResponseDTO> listarEstoqueBaixoPorUnidade(UUID unidadeId) {
		validarTenantUnidade(unidadeId);
		return estoqueCentralRepository.findByUnidadePublicIdAndAtivoTrue(unidadeId).stream()
				.filter(estoque -> estoque.getQuantidadeAtual().compareTo(estoque.getQuantidadeMinima()) <= 0)
				.map(this::comResumoFisico).toList();
	}

	@Transactional
	public void deletar(UUID id) {
		EstoqueCentral estoque = buscarEstoqueNoTenant(id);

		if (!Boolean.TRUE.equals(estoque.getAtivo())) {
			throw new BusinessRuleException("O estoque central já está inativo.");
		}

		estoque.setAtivo(false);
	}


	private List<EstoqueCentralResponseDTO> comResumosFisicos(List<EstoqueCentral> estoques, UUID unidadeId) {
		Map<Long, List<RecipienteEstoque>> porEstoque = new LinkedHashMap<>();
		for (RecipienteEstoque recipiente : recipienteEstoqueRepository.buscarPorUnidadeParaResumo(unidadeId)) {
			porEstoque.computeIfAbsent(recipiente.getLote().getEstoqueCentral().getId(), ignorado -> new ArrayList<>())
					.add(recipiente);
		}
		return estoques.stream().map(estoque ->
				montarResumoFisico(estoque, porEstoque.getOrDefault(estoque.getId(), List.of()))).toList();
	}

	private EstoqueCentralResponseDTO comResumoFisico(EstoqueCentral estoque) {
		return montarResumoFisico(estoque,
				recipienteEstoqueRepository.findByLoteEstoqueCentralIdOrderByLoteIdAscSequencialAsc(estoque.getId()));
	}

	private EstoqueCentralResponseDTO montarResumoFisico(EstoqueCentral estoque, List<RecipienteEstoque> recipientes) {
		EstoqueCentralResponseDTO dto = new EstoqueCentralResponseDTO(estoque);
		Map<String, EstoqueCentralResponseDTO.ApresentacaoFisicaDTO> porApresentacao = new LinkedHashMap<>();
		for (RecipienteEstoque recipiente : recipientes) {
			if (!Boolean.TRUE.equals(recipiente.getLote().getAtivo())) continue;
			String chave = recipiente.getTipoEmbalagem().name() + ":"
					+ recipiente.getCapacidadeInicial().stripTrailingZeros().toPlainString();
			var resumo = porApresentacao.computeIfAbsent(chave, ignorado ->
					new EstoqueCentralResponseDTO.ApresentacaoFisicaDTO(
						recipiente.getTipoEmbalagem().name(), recipiente.getCapacidadeInicial(),
						recipiente.getUnidadeMedida().name(), 0, 0, 0));
			switch (recipiente.getEstado()) {
				case FECHADO -> resumo.setFechados(resumo.getFechados() + 1);
				case ABERTO -> resumo.setAbertos(resumo.getAbertos() + 1);
				case ESGOTADO -> resumo.setEsgotados(resumo.getEsgotados() + 1);
			}
		}
		dto.setApresentacoesFisicas(new ArrayList<>(porApresentacao.values()));
		return dto;
	}

	private EstoqueCentral buscarEstoqueNoTenant(UUID id) {
		// Correção de segurança: o "else" antigo caía para uma busca sem
		// filtro de unidade (findByPublicId) quando não havia tenant ativo,
		// permitindo que qualquer um lesse o estoque de outra unidade só
		// deixando de enviar o header. Agora exigimos o tenant primeiro.
		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
		return estoqueCentralRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));
	}

	private void validarTenantUnidade(UUID unidadeId) {
		if (!TenantContext.pertence(unidadeId)) {
			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	/**
	 * Garante que existe uma unidade (tenant) definida para a requisição atual, ou
	 * seja, que o header X-SGL-Unidade-Id foi enviado e é válido.
	 *
	 * Antes desta correção, vários métodos deste service tinham um "modo sem
	 * tenant": se o header não viesse, a consulta virava uma busca global,
	 * devolvendo dados de todas as unidades. Como o sistema ainda não tem
	 * autenticação de verdade (ver TenantContext.pertence), esse "modo sem tenant"
	 * era, na prática, uma porta aberta para vazar dados entre unidades. Agora, sem
	 * tenant definido, a operação é negada.
	 */
	private void exigirTenantAtivo() {
		if (!TenantContext.ativo()) {
			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private Unidade buscarUnidade(UUID unidadeId) {
		return unidadeRepository.findByPublicId(unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
	}
}
