package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.DescarteProdutoDTO;
import com.sgl.dto.EstoqueCentralDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
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

import lombok.RequiredArgsConstructor;

/**
 * Gerencia cadastro, configuração e consulta do saldo agregado de materiais de
 * uma Unidade.
 *
 * <p>Com a introdução de lotes, o saldo nasce em zero e deve ser alimentado
 * pelas operações físicas coordenadas por MovimentacaoEstoqueService.</p>
 */
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
            throw new BusinessRuleException(
                    "Já existe estoque para esse produto nesta unidade."
            );
        }

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Unidade",
                        dto.getUnidadeId()
                ));

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produto",
                        dto.getProdutoId()
                ));

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
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new ResourceNotFoundException("Unidade", unidadeId);
        }

        return estoqueCentralRepository.findByUnidadeId(unidadeId).stream()
                .map(EstoqueCentralDTO::new)
                .toList();
    }

    @Transactional
    public EstoqueCentralDTO atualizar(Long id, EstoqueCentralDTO dto) {
        EstoqueCentral estoque = estoqueCentralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

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

    /**
     * Fluxo legado mantido apenas até a migração dos testes e consumidores para
     * entrada física por lote em MovimentacaoEstoqueService.
     */
    @Deprecated
    @Transactional
    public EstoqueCentralDTO entrada(Long id, MovimentacaoEstoqueDTO dto) {
        validarQuantidade(dto.getQuantidadeMovimentada());

        EstoqueCentral estoque = estoqueCentralRepository.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário",
                        dto.getUsuarioId()
                ));

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual = quantidadeAnterior + dto.getQuantidadeMovimentada();

        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(estoque.getProduto())
                .usuario(usuario)
                .estoqueCentral(estoque)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(dto.getOrigem())
                .quantidadeMovimentada(dto.getQuantidadeMovimentada())
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeAtual)
                .dataMovimentacao(LocalDateTime.now())
                .observacao(dto.getObservacao())
                .build();

        movimentacaoEstoqueRepository.save(movimentacao);
        return new EstoqueCentralDTO(estoque);
    }

    /**
     * Fluxo legado. A saída definitiva será migrada para consumo de lotes por
     * FEFO/FIFO em MovimentacaoEstoqueService.
     */
    @Deprecated
    @Transactional
    public EstoqueCentralDTO saida(Long id, MovimentacaoEstoqueDTO dto) {
        validarQuantidade(dto.getQuantidadeMovimentada());

        EstoqueCentral estoque = estoqueCentralRepository.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", id));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário",
                        dto.getUsuarioId()
                ));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException(
                    "O usuário responsável pela movimentação está inativo."
            );
        }

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeMovimentada = dto.getQuantidadeMovimentada();

        if (quantidadeAnterior < quantidadeMovimentada) {
            throw new BusinessRuleException(
                    "Estoque insuficiente. Disponível: "
                            + quantidadeAnterior
                            + ", solicitado: "
                            + quantidadeMovimentada
            );
        }

        int quantidadeAtual = quantidadeAnterior - quantidadeMovimentada;
        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(estoque.getProduto())
                .usuario(usuario)
                .estoqueCentral(estoque)
                .tipoMovimentacao(TipoMovimentacao.SAIDA)
                .origem(dto.getOrigem())
                .quantidadeMovimentada(quantidadeMovimentada)
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
            throw new ResourceNotFoundException("Estoque central", id);
        }
        estoqueCentralRepository.deleteById(id);
    }

    /**
     * Fluxo legado. O descarte será migrado para lote específico para que a
     * validade usada seja a validade do lote e não a validade antiga de Produto.
     */
    @Deprecated
    @Transactional
    public EstoqueCentralDTO descartarProdutoVencido(
            Long estoqueId,
            DescarteProdutoDTO dto) {

        EstoqueCentral estoque = estoqueCentralRepository
                .buscarPorIdComBloqueio(estoqueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Estoque central", estoqueId));

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário",
                        dto.getUsuarioId()
                ));

        Produto produto = estoque.getProduto();

        if (!Boolean.TRUE.equals(produto.getPerecivel())) {
            throw new BusinessRuleException(
                    "Somente produtos perecíveis podem ser descartados por validade."
            );
        }

        if (produto.getDataValidade() == null
                || !produto.getDataValidade().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("O produto ainda não está vencido.");
        }

        if (dto.getQuantidade() > estoque.getQuantidadeAtual()) {
            throw new BusinessRuleException(
                    "Quantidade de descarte maior que o estoque disponível."
            );
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
            throw new BusinessRuleException("A quantidade deve ser maior que zero.");
        }
    }
}
