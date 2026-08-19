package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

    private final EstoqueCentralRepository estoqueCentralRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;

    @Transactional
    public EstoqueCentralDTO criar(EstoqueCentralDTO dto) {
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

        return new EstoqueCentralDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarTodos() {
        return estoqueCentralRepository.findAll().stream()
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public EstoqueCentralDTO buscarPorId(UUID id) {
        EstoqueCentral estoque = estoqueCentralRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public EstoqueCentralDTO buscarPorUnidadeEProduto(UUID unidadeId, UUID produtoId) {
        Unidade unidade = buscarUnidade(unidadeId);
        Produto produto = produtoRepository.findByPublicId(produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", produtoId));

        EstoqueCentral estoque = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estoque da unidade " + unidadeId + " para o produto " + produtoId
                ));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarPorUnidade(UUID unidadeId) {
        Unidade unidade = buscarUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadeId(unidade.getId()).stream()
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralDTO atualizar(UUID id, EstoqueCentralDTO dto) {
        EstoqueCentral estoque = estoqueCentralRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
        if (dto.getAtivo() != null) {
            estoque.setAtivo(dto.getAtivo());
        }

        return new EstoqueCentralDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarEstoqueBaixoPorUnidade(UUID unidadeId) {
        Unidade unidade = buscarUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidade.getId()).stream()
                .filter(estoque -> estoque.getQuantidadeAtual() <= estoque.getQuantidadeMinima())
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public void deletar(UUID id) {
        EstoqueCentral estoque = estoqueCentralRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        if (!Boolean.TRUE.equals(estoque.getAtivo())) {
            throw new BusinessRuleException("O estoque central já está inativo.");
        }

        estoque.setAtivo(false);
    }

    private Unidade buscarUnidade(UUID unidadeId) {
        return unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
    }
}
