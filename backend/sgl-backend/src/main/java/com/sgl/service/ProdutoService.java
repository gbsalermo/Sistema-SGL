package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProdutoDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Produto;
import com.sgl.model.enums.NivelRisco;
import com.sgl.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    @Transactional
    public ProdutoDTO criar(ProdutoDTO dto) {
        Produto produto = new Produto();
        preencherProduto(produto, dto);
        return new ProdutoDTO(produtoRepository.save(produto));
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarTodos() {
        return produtoRepository.findAll().stream()
                .map(ProdutoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarPorRisco(NivelRisco risco) {
        return produtoRepository.findByRisco(risco).stream()
                .map(ProdutoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> listarPereciveis() {
        return produtoRepository.findByPerecivelTrue().stream()
                .map(ProdutoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoDTO> buscarPorNome(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome).stream()
                .map(ProdutoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoDTO buscarPorId(UUID id) {
        Produto produto = produtoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        return new ProdutoDTO(produto);
    }

    @Transactional
    public ProdutoDTO atualizar(UUID id, ProdutoDTO dto) {
        Produto produto = produtoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));

        preencherProduto(produto, dto);
        return new ProdutoDTO(produtoRepository.save(produto));
    }

    @Transactional
    public void deletar(UUID id) {
        Produto produto = produtoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto", id));
        produto.setAtivo(false);
    }

    private void preencherProduto(Produto produto, ProdutoDTO dto) {
        validarCodigoReferencia(produto, dto.getCodigoReferencia());

        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setCodigoReferencia(dto.getCodigoReferencia());
        produto.setUnidadeMedida(dto.getUnidadeMedida());
        produto.setLocalizacaoFisica(dto.getLocalizacaoFisica());

        preencherRisco(produto, dto);
        preencherDadosPereciveis(produto, dto);

        produto.setCondicoesArmazenamento(dto.getCondicoesArmazenamento());
        produto.setUnidadeArmazenamento(dto.getUnidadeArmazenamento());

        if (produto.getId() == null) {
            produto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        } else if (dto.getAtivo() != null) {
            produto.setAtivo(dto.getAtivo());
        }
    }

    private void validarCodigoReferencia(Produto produto, String codigoReferencia) {
        boolean codigoDuplicado;

        if (produto.getId() == null) {
            codigoDuplicado = produtoRepository.existsByCodigoReferencia(codigoReferencia);
        } else {
            codigoDuplicado = produtoRepository.existsByCodigoReferenciaAndIdNot(
                    codigoReferencia,
                    produto.getId()
            );
        }

        if (codigoDuplicado) {
            throw new BusinessRuleException(
                    "Já existe um produto com este código de referência."
            );
        }
    }

    private void preencherRisco(Produto produto, ProdutoDTO dto) {
        NivelRisco risco = dto.getRisco();

        if (risco == null) {
            throw new BusinessRuleException("O nível de risco é obrigatório.");
        }

        produto.setRisco(risco);

        if (risco == NivelRisco.NENHUM) {
            produto.setTipoRisco(null);
            produto.setDescricaoRisco(null);
            return;
        }

        if (dto.getTipoRisco() == null) {
            throw new BusinessRuleException(
                    "O tipo de risco é obrigatório para produtos com risco."
            );
        }

        produto.setTipoRisco(dto.getTipoRisco());
        produto.setDescricaoRisco(dto.getDescricaoRisco());
    }

    private void preencherDadosPereciveis(Produto produto, ProdutoDTO dto) {
        boolean perecivel = Boolean.TRUE.equals(dto.getPerecivel());
        produto.setPerecivel(perecivel);

        if (!perecivel) {
            produto.setTipoPerecivel(null);
            return;
        }

        if (dto.getTipoPerecivel() == null) {
            throw new BusinessRuleException(
                    "O tipo de perecível é obrigatório para produtos perecíveis."
            );
        }

        produto.setTipoPerecivel(dto.getTipoPerecivel());
    }
}
