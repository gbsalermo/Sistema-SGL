package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.DescarteProdutoDTO;
import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.model.enums.OrigemMovimentacao;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstoqueCentralService {

    private final EstoqueCentralRepository estoqueCentralRepository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public EstoqueCentralDTO criar(EstoqueCentralDTO dto) {
        if (estoqueCentralRepository.existsByUnidadeIdAndProdutoId(
                dto.getUnidadeId(), dto.getProdutoId())) {
            throw new IllegalArgumentException(
                    "Já existe estoque para esse produto nesta unidade.");
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Unidade não encontrada com id: " + dto.getUnidadeId()));

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Produto não encontrado com id: " + dto.getProdutoId()));

        EstoqueCentral estoque = EstoqueCentral.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeAtual(dto.getQuantidadeAtual())
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
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque central não encontrado com id: " + id));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public EstoqueCentralDTO buscarPorUnidadeEProduto(Long unidadeId, Long produtoId) {
        EstoqueCentral estoque = estoqueCentralRepository
                .findByUnidadeIdAndProdutoId(unidadeId, produtoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque não encontrado para a unidade "
                                + unidadeId + " e produto " + produtoId));
        return new EstoqueCentralDTO(estoque);
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarPorUnidade(Long unidadeId) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new EntityNotFoundException(
                    "Unidade não encontrada com id: " + unidadeId);
        }

        return estoqueCentralRepository.findByUnidadeId(unidadeId).stream()
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralDTO atualizar(Long id, EstoqueCentralDTO dto) {
        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque central não encontrado"));

        estoque.setQuantidadeMinima(dto.getQuantidadeMinima());
        estoque.setAtivo(dto.getAtivo());
        return new EstoqueCentralDTO(estoqueCentralRepository.save(estoque));
    }

    @Transactional(readOnly = true)
    public List<EstoqueCentralDTO> listarEstoqueBaixoPorUnidade(Long unidadeId) {
        return estoqueCentralRepository.findByUnidadeIdAndAtivoTrue(unidadeId).stream()
                .filter(estoque -> estoque.getQuantidadeAtual()
                        <= estoque.getQuantidadeMinima())
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralDTO entrada(
            Long id,
            MovimentacaoEstoqueDTO dto) {

        validarQuantidade(dto.getQuantidadeMovimentada());

        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque central não encontrado com id: " + id
                ));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()
                ));

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual =
                quantidadeAnterior + dto.getQuantidadeMovimentada();

        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao =
                MovimentacaoEstoque.builder()
                        .produto(estoque.getProduto())
                        .usuario(usuario)
                        .estoqueCentral(estoque)
                        .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                        .origem(dto.getOrigem())
                        .quantidadeMovimentada(
                                dto.getQuantidadeMovimentada()
                        )
                        .quantidadeAnterior(quantidadeAnterior)
                        .quantidadeAtual(quantidadeAtual)
                        .dataMovimentacao(LocalDateTime.now())
                        .observacao(dto.getObservacao())
                        .build();

        movimentacaoEstoqueRepository.save(movimentacao);

        return new EstoqueCentralDTO(estoque);
    }

    @Transactional
    public EstoqueCentralDTO saida(
            Long id,
            MovimentacaoEstoqueDTO dto) {

        validarQuantidade(dto.getQuantidadeMovimentada());

        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque central não encontrado com id: " + id
                ));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()
                ));
        Usuario usuarioAprovador = usuarioRepository
                .findById(dto.getU)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário aprovador não encontrado com id: "
                                + dto.getUsuarioAprovadorId()
                ));

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual =
                quantidadeAnterior - dto.getQuantidadeMovimentada();

        if (quantidadeAtual < 0) {
            throw new IllegalArgumentException(
                    "Estoque insuficiente. Disponível: "
                            + quantidadeAnterior
            );
        }

        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao =
                MovimentacaoEstoque.builder()
                        .produto(estoque.getProduto())
                        .usuario(usuario)
                        .estoqueCentral(estoque)
                        .tipoMovimentacao(TipoMovimentacao.SAIDA)
                        .origem(dto.getOrigem())
                        .quantidadeMovimentada(
                                dto.getQuantidadeMovimentada()
                        )
                        .quantidadeAnterior(quantidadeAnterior)
                        .quantidadeAtual(quantidadeAtual)
                        .dataMovimentacao(LocalDateTime.now())
                        .observacao(dto.getObservacao())
                        .build();

        movimentacaoEstoqueRepository.save(movimentacao);

        return new EstoqueCentralDTO(estoque);
    }

    @Transactional
    public void deletar(Long id) {
        if (!estoqueCentralRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Estoque central não encontrado com id: " + id);
        }
        estoqueCentralRepository.deleteById(id);
    }

    @Transactional
    public EstoqueCentralDTO descartarProdutoVencido(
            Long estoqueId,
            DescarteProdutoDTO dto) {

        EstoqueCentral estoque = estoqueCentralRepository.findById(estoqueId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque não encontrado com id: " + estoqueId));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com id: " + dto.getUsuarioId()));

        Produto produto = estoque.getProduto();

        if (!Boolean.TRUE.equals(produto.getPerecivel())) {
            throw new IllegalArgumentException(
                    "Somente produtos perecíveis podem ser descartados por validade.");
        }

        if (produto.getDataValidade() == null
                || !produto.getDataValidade().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "O produto ainda não está vencido.");
        }

        if (dto.getQuantidade() > estoque.getQuantidadeAtual()) {
            throw new IllegalArgumentException(
                    "Quantidade de descarte maior que o estoque disponível.");
        }

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual = quantidadeAnterior - dto.getQuantidade();
        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(produto)
                .usuario(usuario)
                .tipoMovimentacao(TipoMovimentacao.DESCARTE_VENCIMENTO)
                .origem(OrigemMovimentacao.DESCARTE)
                .quantidadeMovimentada(dto.getQuantidade())
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeAtual)
                .dataMovimentacao(LocalDateTime.now())
                .observacao(dto.getJustificativa())
                .estoqueCentral(estoque)
                .build();

        movimentacaoEstoqueRepository.save(movimentacao);
        return new EstoqueCentralDTO(estoque);
    }
    
    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }
    }
}
