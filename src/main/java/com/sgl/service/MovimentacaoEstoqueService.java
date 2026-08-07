package com.sgl.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.EntradaLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Produto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

/**
 * Centraliza as operações físicas que alteram o estoque e mantém
 * MovimentacaoEstoque como trilha de auditoria.
 */
@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;
    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarTodos() {
        return movimentacaoRepository.findAll().stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovimentacaoEstoqueDTO buscarPorId(Long id) {
        return movimentacaoRepository.findById(id)
                .map(MovimentacaoEstoqueDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação", id));
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorProduto(Long produtoId) {
        return movimentacaoRepository.findByProdutoId(produtoId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorLaboratorio(Long laboratorioId) {
        return movimentacaoRepository.findByLaboratorioId(laboratorioId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorUsuario(Long usuarioId) {
        return movimentacaoRepository.findByUsuarioId(usuarioId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorPedido(Long pedidoId) {
        return movimentacaoRepository.findByPedidoId(pedidoId).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorTipo(TipoMovimentacao tipo) {
        return movimentacaoRepository.findByTipoMovimentacao(tipo).stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }

    /**
     * Registra uma entrada física por lote.
     *
     * O usuário é recebido como entidade para que a origem da identidade fique
     * desacoplada deste caso de uso. Durante os testes locais ele virá da
     * autenticação simulada; futuramente virá do contexto autenticado pela API
     * corporativa.
     */
    @Transactional
    public LoteDTO registrarEntradaLote(
            Long estoqueId,
            EntradaLoteDTO dto,
            Usuario usuario) {

        validarUsuarioResponsavel(usuario);

        EstoqueCentral estoque = estoqueCentralRepository
                .buscarPorIdComBloqueio(estoqueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Estoque central", estoqueId));

        if (!Boolean.TRUE.equals(estoque.getAtivo())) {
            throw new BusinessRuleException("O estoque informado está inativo.");
        }

        validarEntradaLote(estoque.getProduto(), dto);

        if (loteRepository.existsByEstoqueCentralIdAndNumeroLote(
                estoqueId,
                dto.getNumeroLote())) {
            throw new BusinessRuleException(
                    "Já existe lote com esse número neste estoque."
            );
        }

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int quantidadeAtual = quantidadeAnterior + dto.getQuantidade();

        Lote lote = new Lote();
        lote.setEstoqueCentral(estoque);
        lote.setNumeroLote(dto.getNumeroLote());
        lote.setQuantidadeInicial(dto.getQuantidade());
        lote.setQuantidadeDisponivel(dto.getQuantidade());
        lote.setDataEntrada(LocalDate.now());
        lote.setDataValidade(dto.getDataValidade());
        lote.setAtivo(true);

        loteRepository.save(lote);

        estoque.setQuantidadeAtual(quantidadeAtual);
        estoqueCentralRepository.save(estoque);

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .produto(estoque.getProduto())
                .usuario(usuario)
                .estoqueCentral(estoque)
                .tipoMovimentacao(TipoMovimentacao.ENTRADA)
                .origem(dto.getOrigem())
                .quantidadeMovimentada(dto.getQuantidade())
                .quantidadeAnterior(quantidadeAnterior)
                .quantidadeAtual(quantidadeAtual)
                .dataMovimentacao(LocalDateTime.now())
                .observacao(dto.getObservacao())
                .build();

        movimentacaoRepository.save(movimentacao);
        return new LoteDTO(lote);
    }

    /**
     * Define a política de consumo do estoque.
     * Produto perecível usa FEFO; produto não perecível usa FIFO.
     */
    @Transactional
    public List<LoteDTO> listarLotesOrdenadosParaSaida(Long estoqueId) {
        EstoqueCentral estoque = estoqueCentralRepository
                .buscarPorIdComBloqueio(estoqueId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Estoque central", estoqueId));

        List<Lote> lotes;

        if (Boolean.TRUE.equals(estoque.getProduto().getPerecivel())) {
            lotes = loteRepository.buscarDisponiveisPorFefoComBloqueio(
                    estoqueId,
                    LocalDate.now()
            );
        } else {
            lotes = loteRepository.buscarDisponiveisPorEntradaComBloqueio(estoqueId);
        }

        return lotes.stream()
                .map(LoteDTO::new)
                .toList();
    }

    private void validarEntradaLote(Produto produto, EntradaLoteDTO dto) {
        if (Boolean.TRUE.equals(produto.getPerecivel())) {
            if (dto.getDataValidade() == null) {
                throw new BusinessRuleException(
                        "Data de validade é obrigatória para produto perecível."
                );
            }

            if (dto.getDataValidade().isBefore(LocalDate.now())) {
                throw new BusinessRuleException(
                        "Não é possível registrar entrada de lote já vencido."
                );
            }
        } else if (dto.getDataValidade() != null) {
            throw new BusinessRuleException(
                    "Produto não perecível não deve possuir data de validade no lote."
            );
        }
    }

    private void validarUsuarioResponsavel(Usuario usuario) {
        if (usuario == null) {
            throw new BusinessRuleException("Usuário responsável é obrigatório.");
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new BusinessRuleException(
                    "O usuário responsável pela movimentação está inativo."
            );
        }
    }
}
