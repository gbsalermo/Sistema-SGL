package com.sgl.service;

import java.util.List;

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
        if (estoqueCentralRepository.existsByUnidadeIdAndProdutoId(dto.getUnidadeId(), dto.getProdutoId())) {
            throw new BusinessRuleException("Já existe estoque para esse produto nesta unidade.");
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", dto.getProdutoId()));

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
    public EstoqueCentralDTO buscarPorId(Long id) {
        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public EstoqueCentralDTO buscarPorUnidadeEProduto(Long unidadeId, Long produtoId) {
        EstoqueCentral estoque = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estoque da unidade " + unidadeId + " para o produto " + produtoId
                ));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarPorUnidade(Long unidadeId) {
        validarUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadeId(unidadeId).stream()
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralDTO atualizar(Long id, EstoqueCentralDTO dto) {
        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
        if (dto.getAtivo() != null) {
            estoque.setAtivo(dto.getAtivo());
        }

        return new EstoqueCentralDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarEstoqueBaixoPorUnidade(Long unidadeId) {
        validarUnidade(unidadeId);
        return estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidadeId).stream()
                .filter(estoque -> estoque.getQuantidadeAtual() <= estoque.getQuantidadeMinima())
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public void deletar(Long id) {
        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        if (!Boolean.TRUE.equals(estoque.getAtivo())) {
            throw new BusinessRuleException("O estoque central já está inativo.");
        }

        estoque.setAtivo(false);
    }

    private void validarUnidade(Long unidadeId) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new ResourceNotFoundException("Unidade", unidadeId);
        }
    }
}
