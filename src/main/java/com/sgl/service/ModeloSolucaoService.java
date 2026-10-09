package com.sgl.service;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.*;
import com.sgl.dto.response.*;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.*;
import com.sgl.model.medida.ConversorUnidadeMedida;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModeloSolucaoService {
    private final ModeloSolucaoRepository repository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public ModeloSolucaoResponseDTO criar(ModeloSolucaoRequestDTO dto) {
        UUID tenant = exigirTenant();
        if (!tenant.equals(dto.getUnidadeId())) throw new BusinessRuleException("Unidade da solução não corresponde à unidade atual.");
        Unidade unidade = unidadeRepository.findByPublicId(tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Unidade", tenant));
        String nome = validarNome(dto.getNome());
        if (repository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), nome))
            throw new BusinessRuleException("Já existe um modelo de solução com este nome na unidade.");
        ModeloSolucao modelo = ModeloSolucao.builder().unidade(unidade).nome(nome)
            .descricao(dto.getDescricao()).instrucoesPreparo(dto.getInstrucoesPreparo())
            .ativo(dto.getAtivo() == null || dto.getAtivo()).build();
        preencherComponentes(modelo, dto.getComponentes(), tenant);
        return new ModeloSolucaoResponseDTO(repository.save(modelo));
    }

    @Transactional(readOnly=true)
    public List<ModeloSolucaoResponseDTO> listar(boolean somenteAtivos) {
        UUID tenant = exigirTenant();
        List<ModeloSolucao> modelos = somenteAtivos
            ? repository.findDistinctByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(tenant)
            : repository.findDistinctByUnidadePublicIdOrderByNomeAsc(tenant);
        return modelos.stream().map(ModeloSolucaoResponseDTO::new).toList();
    }

    @Transactional(readOnly=true)
    public ModeloSolucaoResponseDTO buscar(UUID id) {
        return new ModeloSolucaoResponseDTO(buscarEntidade(id));
    }

    @Transactional
    public ModeloSolucaoResponseDTO atualizar(UUID id, ModeloSolucaoRequestDTO dto) {
        ModeloSolucao modelo = buscarEntidade(id);
        UUID tenant = exigirTenant();
        if (!tenant.equals(dto.getUnidadeId()))
            throw new BusinessRuleException("O modelo não pode ser transferido para outra unidade.");
        String nome = validarNome(dto.getNome());
        if (repository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(modelo.getUnidade().getId(), nome, modelo.getId()))
            throw new BusinessRuleException("Já existe um modelo de solução com este nome na unidade.");
        // Reutilizar os registros de componentes existentes para evitar conflitos de unicidade
        // quando a mesma receita for atualizada, sem excluir e reinserir a mesma linha.
        List<ComponenteModeloSolucao> novos = montarComponentes(dto.getComponentes(), tenant);
        Map<Long, ComponenteModeloSolucao> antigos = new HashMap<>();
        for (ComponenteModeloSolucao antigo : modelo.getComponentes()) {
            antigos.put(antigo.getProduto().getId(), antigo);
        }
        Set<Long> mantidos = new HashSet<>();
        for (ComponenteModeloSolucao novo : novos) {
            Long produtoId = novo.getProduto().getId();
            mantidos.add(produtoId);
            ComponenteModeloSolucao existente = antigos.get(produtoId);
            if (existente != null) {
                existente.setQuantidade(novo.getQuantidade());
                existente.setUnidadeMedida(novo.getUnidadeMedida());
                existente.setOrdem(novo.getOrdem());
            } else {
                modelo.adicionarComponente(novo);
            }
        }
        modelo.getComponentes().removeIf(c -> !mantidos.contains(c.getProduto().getId()));
        modelo.setNome(nome);
        modelo.setDescricao(dto.getDescricao());
        modelo.setInstrucoesPreparo(dto.getInstrucoesPreparo());
        if (dto.getAtivo() != null) modelo.setAtivo(dto.getAtivo());
        return new ModeloSolucaoResponseDTO(repository.save(modelo));
    }

    @Transactional
    public void inativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    @Transactional(readOnly=true)
    public ModeloSolucao buscarEntidade(UUID id) {
        UUID tenant = exigirTenant();
        return repository.findByPublicIdAndUnidadePublicId(id, tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Modelo de solução", id));
    }

    private void preencherComponentes(ModeloSolucao modelo, List<ComponenteModeloSolucaoRequestDTO> itens, UUID tenant) {
        montarComponentes(itens, tenant).forEach(modelo::adicionarComponente);
    }

    private List<ComponenteModeloSolucao> montarComponentes(List<ComponenteModeloSolucaoRequestDTO> itens, UUID tenant) {
        if (itens == null || itens.isEmpty()) throw new BusinessRuleException("Informe os produtos da solução.");
        List<ComponenteModeloSolucao> componentes = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();
        int ordem=1;
        for (ComponenteModeloSolucaoRequestDTO item : itens) {
            if (item == null || item.getProdutoId() == null || item.getQuantidade() == null
                || item.getQuantidade().compareTo(BigDecimal.ZERO) <= 0 || item.getUnidadeMedida() == null)
                throw new BusinessRuleException("Produto, quantidade positiva e unidade são obrigatórios na composição.");
            if (!ids.add(item.getProdutoId()))
                throw new BusinessRuleException("O mesmo produto não pode ser duplicado em uma solução.");
            if (!produtoRepository.pertenceAUnidade(item.getProdutoId(), tenant))
                throw new ResourceNotFoundException("Produto", item.getProdutoId());
            Produto produto = produtoRepository.findByPublicId(item.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", item.getProdutoId()));
            produto.validateActive();
            BigDecimal canonica = ConversorUnidadeMedida.converterParaCanonica(
                item.getQuantidade(), item.getUnidadeMedida(), produto.getUnidadeMedida());
            if (canonica.scale()>6 && canonica.stripTrailingZeros().scale()>6)
                throw new BusinessRuleException("A quantidade do componente excede a precisão de estoque (6 casas decimais).");
            componentes.add(ComponenteModeloSolucao.builder()
                .produto(produto).ordem(ordem++).quantidade(item.getQuantidade())
                .unidadeMedida(item.getUnidadeMedida()).build());
        }
        return componentes;
    }

    private String validarNome(String s) {
        if (s == null || s.isBlank() || s.trim().length() > 150)
            throw new BusinessRuleException("Nome do modelo de solução é obrigatório e deve ter até 150 caracteres.");
        return s.trim();
    }

    private UUID exigirTenant() {
        if (!TenantContext.ativo())
            throw new BusinessRuleException("Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        return TenantContext.unidadeAtual().orElseThrow();
    }
}

