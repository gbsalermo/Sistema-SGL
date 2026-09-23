package com.sgl.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ComponenteModeloResiduoRequestDTO;
import com.sgl.dto.request.ModeloResiduoRequestDTO;
import com.sgl.dto.response.ModeloResiduoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ClasseResiduo;
import com.sgl.model.ComponenteModeloResiduo;
import com.sgl.model.ModeloResiduo;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.enums.MedidaSeguranca;
import com.sgl.repository.ClasseResiduoRepository;
import com.sgl.repository.ModeloResiduoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModeloResiduoService {

	private final ModeloResiduoRepository modeloResiduoRepository;
	private final UnidadeRepository unidadeRepository;
	private final ClasseResiduoRepository classeResiduoRepository;
	private final ProdutoRepository produtoRepository;

	@Transactional
	public ModeloResiduoResponseDTO criar(ModeloResiduoRequestDTO dto) {

		exigirTenantAtivo();

		validarTenantUnidade(dto.getUnidadeId());

		Unidade unidade = buscarUnidade(dto.getUnidadeId());

		String nome = normalizarObrigatorio(dto.getNome());

		validarNomeDuplicado(unidade, nome, null);

		ModeloResiduo modelo = ModeloResiduo.builder().unidade(unidade).nome(nome)
				.ativo(dto.getAtivo() != null ? dto.getAtivo() : true).build();

		aplicarDados(modelo, dto, unidade);

		return new ModeloResiduoResponseDTO(modeloResiduoRepository.save(modelo));
	}

	@Transactional(readOnly = true)
	public List<ModeloResiduoResponseDTO> listarTodos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return modeloResiduoRepository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream()
				.map(ModeloResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public List<ModeloResiduoResponseDTO> listarAtivos() {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return modeloResiduoRepository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
				.map(ModeloResiduoResponseDTO::new).toList();
	}

	@Transactional(readOnly = true)
	public ModeloResiduoResponseDTO buscarPorId(UUID id) {

		return new ModeloResiduoResponseDTO(buscarModeloNoTenant(id));
	}

	@Transactional
	public ModeloResiduoResponseDTO atualizar(UUID id, ModeloResiduoRequestDTO dto) {

		ModeloResiduo modelo = buscarModeloNoTenant(id);

		if (!modelo.getUnidade().getPublicId().equals(dto.getUnidadeId())) {

			throw new BusinessRuleException("O modelo de resíduo não pode ser transferido para outra unidade.");
		}

		String nome = normalizarObrigatorio(dto.getNome());

		validarNomeDuplicado(modelo.getUnidade(), nome, modelo.getId());

		modelo.setNome(nome);

		aplicarDados(modelo, dto, modelo.getUnidade());

		if (dto.getAtivo() != null) {
			modelo.setAtivo(dto.getAtivo());
		}

		return new ModeloResiduoResponseDTO(modeloResiduoRepository.save(modelo));
	}

	@Transactional
	public void deletar(UUID id) {

		ModeloResiduo modelo = buscarModeloNoTenant(id);

		modelo.setAtivo(false);
	}

	private void aplicarDados(ModeloResiduo modelo, ModeloResiduoRequestDTO dto, Unidade unidade) {

		validarTratamento(dto.getTratamentoRealizado(), dto.getDescricaoTratamento());

		validarRiscos(dto.getRiscos());

		validarSeguranca(dto.getMedidasSeguranca(), dto.getObservacaoSeguranca());

		List<ClasseResiduo> classes = buscarClassesAtivasDaUnidade(dto.getClassesIds(), unidade.getPublicId());

		modelo.setDescricao(normalizarObrigatorio(dto.getDescricao()));

		modelo.setProcessoOrigem(normalizarObrigatorio(dto.getProcessoOrigem()));

		modelo.setEstadoFisico(dto.getEstadoFisico());

		modelo.setTratamentoRealizado(dto.getTratamentoRealizado());

		modelo.setDescricaoTratamento(
				Boolean.TRUE.equals(dto.getTratamentoRealizado()) ? normalizarOpcional(dto.getDescricaoTratamento())
						: null);

		modelo.setRecipiente(normalizarObrigatorio(dto.getRecipiente()));

		modelo.setUnidadeMedida(dto.getUnidadeMedida());

		modelo.setNivelRisco(dto.getNivelRisco());

		modelo.getRiscos().clear();
		modelo.getRiscos().addAll(dto.getRiscos());

		modelo.getClasses().clear();
		modelo.getClasses().addAll(classes);

		modelo.getMedidasSeguranca().clear();
		modelo.getMedidasSeguranca().addAll(dto.getMedidasSeguranca());

		modelo.setObservacaoSeguranca(normalizarOpcional(dto.getObservacaoSeguranca()));

		substituirComponentes(modelo, dto.getComponentes(), unidade.getPublicId());
	}

	private void substituirComponentes(ModeloResiduo modelo, List<ComponenteModeloResiduoRequestDTO> itens,
			UUID unidadeId) {

		if (itens == null || itens.isEmpty()) {
			throw new BusinessRuleException("O modelo deve possuir pelo menos um componente.");
		}

		modelo.getComponentes().clear();

		for (ComponenteModeloResiduoRequestDTO item : itens) {

			modelo.addComponente(criarComponente(item, unidadeId));
		}
	}

	private ComponenteModeloResiduo criarComponente(ComponenteModeloResiduoRequestDTO dto, UUID unidadeId) {

		Produto produto = null;
		String nomeComponente = dto.getNomeComponente();

		if (dto.getProdutoId() != null) {

			boolean pertenceUnidade = produtoRepository.pertenceAUnidade(dto.getProdutoId(), unidadeId);

			if (!pertenceUnidade) {
				throw new ResourceNotFoundException("Produto", dto.getProdutoId());
			}

			produto = produtoRepository.findByPublicId(dto.getProdutoId())
					.orElseThrow(() -> new ResourceNotFoundException("Produto", dto.getProdutoId()));

			produto.validateActive();

			if (nomeComponente == null || nomeComponente.isBlank()) {

				nomeComponente = produto.getNome();
			}
		}

		if (nomeComponente == null || nomeComponente.isBlank()) {

			throw new BusinessRuleException("O componente do modelo deve possuir nome ou referência de produto.");
		}

		return ComponenteModeloResiduo.builder().produto(produto).nomeComponente(nomeComponente.trim())
				.principal(Boolean.TRUE.equals(dto.getPrincipal()))
				.concentracaoOuQuantidade(normalizarOpcional(dto.getConcentracaoOuQuantidade()))
				.observacao(normalizarOpcional(dto.getObservacao())).build();
	}

	private List<ClasseResiduo> buscarClassesAtivasDaUnidade(Set<UUID> ids, UUID unidadeId) {

		if (ids == null || ids.isEmpty()) {

			throw new BusinessRuleException("Informe pelo menos uma classe de resíduo.");
		}

		List<ClasseResiduo> classes = classeResiduoRepository.findByPublicIdInAndUnidadePublicId(ids, unidadeId);

		if (classes.size() != ids.size()) {

			throw new BusinessRuleException("Uma ou mais classes de resíduo são inválidas para esta unidade.");
		}

		for (ClasseResiduo classe : classes) {
			classe.validateActive();
		}

		return classes;
	}

	private ModeloResiduo buscarModeloNoTenant(UUID id) {

		exigirTenantAtivo();

		UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();

		return modeloResiduoRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Modelo de resíduo", id));
	}

	private Unidade buscarUnidade(UUID unidadeId) {

		return unidadeRepository.findByPublicId(unidadeId)
				.orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
	}

	private void exigirTenantAtivo() {

		if (!TenantContext.ativo()) {

			throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
		}
	}

	private void validarTenantUnidade(UUID unidadeId) {

		if (!TenantContext.pertence(unidadeId)) {

			throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
		}
	}

	private void validarNomeDuplicado(Unidade unidade, String nome, Long idAtual) {

		boolean duplicado;

		if (idAtual == null) {

			duplicado = modeloResiduoRepository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome);

		} else {

			duplicado = modeloResiduoRepository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidade.getId(), nome,
					idAtual);
		}

		if (duplicado) {

			throw new BusinessRuleException("Já existe um modelo de resíduo com este nome na unidade.");
		}
	}

	private void validarTratamento(Boolean tratamentoRealizado, String descricaoTratamento) {

		if (tratamentoRealizado == null) {

			throw new BusinessRuleException("Informe se o modelo possui tratamento padrão.");
		}

		if (Boolean.TRUE.equals(tratamentoRealizado)
				&& (descricaoTratamento == null || descricaoTratamento.isBlank())) {

			throw new BusinessRuleException(
					"A descrição do tratamento é obrigatória quando o modelo possui tratamento padrão.");
		}
	}

	private void validarRiscos(Set<?> riscos) {

		if (riscos == null || riscos.isEmpty()) {

			throw new BusinessRuleException("Informe pelo menos um risco do modelo.");
		}
	}

	private void validarSeguranca(Set<MedidaSeguranca> medidas, String observacao) {

		if (medidas == null || medidas.isEmpty()) {

			throw new BusinessRuleException("Informe pelo menos uma medida de segurança do modelo.");
		}

		if (medidas.contains(MedidaSeguranca.OUTRO) && (observacao == null || observacao.isBlank())) {

			throw new BusinessRuleException("Descreva a medida de segurança marcada como OUTRO.");
		}
	}

	private String normalizarObrigatorio(String valor) {

		return valor.trim();
	}

	private String normalizarOpcional(String valor) {

		if (valor == null || valor.isBlank()) {
			return null;
		}

		return valor.trim();
	}
}