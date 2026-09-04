package com.sgl.service;

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
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

    private final EstoqueCentralRepository estoqueCentralRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;

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

        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(0)
                .quantidadeMinima(dto.getQuantidadeMinima())
                .ativo(dto.getAtivo() != null ? dto.getAtivo() : true)
                .build();

        return new EstoqueCentralResponseDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralResponseDTO> listarTodos() {
        List<EstoqueCentral> estoques = TenantContext.unidadeAtual()
                .map(estoqueCentralRepository::findByUnidadePublicId)
                .orElseGet(estoqueCentralRepository::findAll);

        return estoques.stream()
                .map(EstoqueCentralResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstoqueCentralResponseDTO buscarPorId(UUID id) {
        return new EstoqueCentralResponseDTO(buscarEstoqueNoTenant(id));
    }

    @Transactional(readOnly = true)
    public EstoqueCentralResponseDTO buscarPorUnidadeEProduto(UUID unidadeId, UUID produtoId) {
        validarTenantUnidade(unidadeId);
        Unidade unidade = buscarUnidade(unidadeId);
        Produto produto = produtoRepository.findByPublicId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        EstoqueCentral estoque = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estoque da unidade " + unidadeId + " para o produto " + produtoId
                ));
        return new EstoqueCentralResponseDTO(estoque);
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralResponseDTO> listarPorUnidade(UUID unidadeId) {
        validarTenantUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadePublicId(unidadeId).stream()
                .map(EstoqueCentralResponseDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralResponseDTO atualizar(UUID id, EstoqueCentralRequestDTO dto) {
        EstoqueCentral estoque = buscarEstoqueNoTenant(id);

        estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
        if (dto.getAtivo() != null) {
            estoque.setAtivo(dto.getAtivo());
        }

        return new EstoqueCentralResponseDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralResponseDTO> listarEstoqueBaixoPorUnidade(UUID unidadeId) {
        validarTenantUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadePublicIdAndAtivoTrue(unidadeId).stream()
                .filter(estoque -> estoque.getQuantidadeAtual() <= estoque.getQuantidadeMinima())
                .map(EstoqueCentralResponseDTO::new)
                .toList();
    }

    @Transactional
    public void deletar(UUID id) {
        EstoqueCentral estoque = buscarEstoqueNoTenant(id);

        if (!Boolean.TRUE.equals(estoque.getAtivo())) {
            throw new BusinessRuleException("O estoque central já está inativo.");
        }

        estoque.setAtivo(false);
    }

    private EstoqueCentral buscarEstoqueNoTenant(UUID id) {
        return TenantContext.unidadeAtual()
                .flatMap(unidadeId -> estoqueCentralRepository.findByPublicIdAndUnidadePublicId(id, unidadeId))
                .orElseGet(() -> {
                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException("Estoque central", id);
                    }
                    return estoqueCentralRepository.findByPublicId(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));
                });
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    private Unidade buscarUnidade(UUID unidadeId) {
        return unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
    }
}
