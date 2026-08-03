package com.sgl.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Laboratorio;
import com.sgl.model.MovimentacaoEstoque;
import com.sgl.model.Pedido;
import com.sgl.model.Produto;
import com.sgl.model.Usuario;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.MovimentacaoEstoqueRepository;
import com.sgl.repository.PedidoRepository;
import com.sgl.repository.ProdutoRepository;
import com.sgl.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    private final ProdutoRepository produtoRepository;

    private final LaboratorioRepository laboratorioRepository;

    private final UsuarioRepository usuarioRepository;

    private final PedidoRepository pedidoRepository;

    private final EstoqueCentralRepository estoqueRepository;
    
    
    @Transactional
    public MovimentacaoEstoqueDTO criar(MovimentacaoEstoqueDTO dto) {

        Produto produto = buscarProduto(dto.getProdutoId());

        Usuario usuario = buscarUsuario(dto.getUsuarioId());

        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

        Pedido pedido = buscarPedido(dto.getPedidoId());

        EstoqueCentral estoque = buscarEstoqueCentral(dto.getEstoqueCentralId());

        validarQuantidade(dto);

        MovimentacaoEstoque movimentacao =
                MovimentacaoEstoque.builder()
                        .produto(produto)
                        .usuario(usuario)
                        .laboratorio(laboratorio)
                        .pedido(pedido)
                        .estoqueCentral(estoque)
                        .build();

        preencherMovimentacao( movimentacao,
                dto,
                produto,
                laboratorio,
                usuario,
                pedido,
                estoque);

        MovimentacaoEstoque salva =
                movimentacaoRepository.save(movimentacao);

        return new MovimentacaoEstoqueDTO(salva);
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarTodos() {

        return movimentacaoRepository.findAll()
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public MovimentacaoEstoqueDTO buscarPorId(Long id) {

        return movimentacaoRepository.findById(id)
                .map(MovimentacaoEstoqueDTO::new)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Movimentação não encontrada."));
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorProduto(Long produtoId){

        return movimentacaoRepository.findByProdutoId(produtoId)
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorLaboratorio(Long laboratorioId){

        return movimentacaoRepository
                .findByLaboratorioId(laboratorioId)
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorUsuario(Long usuarioId){

        return movimentacaoRepository
                .findByUsuarioId(usuarioId)
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorPedido(Long pedidoId){

        return movimentacaoRepository
                .findByPedidoId(pedidoId)
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueDTO> listarPorTipo(
            TipoMovimentacao tipo){

        return movimentacaoRepository
                .findByTipoMovimentacao(tipo)
                .stream()
                .map(MovimentacaoEstoqueDTO::new)
                .toList();
    }
    
    private Produto buscarProduto(Long produtoId) {
        return produtoRepository.findById(produtoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Produto não encontrado com id: " + produtoId));
    }

    private Laboratorio buscarLaboratorio(Long laboratorioId) {
        return laboratorioRepository.findById(laboratorioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Laboratório não encontrado com id: " + laboratorioId));
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com id: " + usuarioId));
    }

    private Pedido buscarPedido(Long pedidoId) {
        if (pedidoId == null) {
            return null;
        }

        return pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Pedido não encontrado com id: " + pedidoId));
    }

    private EstoqueCentral buscarEstoqueCentral(Long estoqueCentralId) {
        return estoqueRepository.findById(estoqueCentralId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Estoque Central não encontrado com id: " + estoqueCentralId));
    }

    private void preencherMovimentacao(MovimentacaoEstoque movimentacao,
            MovimentacaoEstoqueDTO dto,
            Produto produto,
            Laboratorio laboratorio,
            Usuario usuario,
            Pedido pedido,
            EstoqueCentral estoqueCentral) {

			movimentacao.setProduto(produto);
			
			movimentacao.setLaboratorio(laboratorio);
			
			movimentacao.setUsuario(usuario);
			
			movimentacao.setPedido(pedido);
			
			movimentacao.setEstoqueCentral(estoqueCentral);
			
			movimentacao.setTipoMovimentacao(dto.getTipoMovimentacao());
			
			movimentacao.setQuantidadeMovimentada(dto.getQuantidadeMovimentada());
			
			movimentacao.setQuantidadeAnterior(estoqueCentral.getQuantidadeAtual());
			
			if (dto.getTipoMovimentacao() == TipoMovimentacao.ENTRADA) {
			
			movimentacao.setQuantidadeAtual(
			estoqueCentral.getQuantidadeAtual() + dto.getQuantidadeMovimentada());
			
			estoqueCentral.setQuantidadeAtual(
			estoqueCentral.getQuantidadeAtual() + dto.getQuantidadeMovimentada());
			
			} else {
			
			if (estoqueCentral.getQuantidadeAtual() < dto.getQuantidadeMovimentada()) {
			throw new IllegalArgumentException(
			"Quantidade insuficiente no estoque.");
			}
			
			movimentacao.setQuantidadeAtual(
			estoqueCentral.getQuantidadeAtual() - dto.getQuantidadeMovimentada());
			
			estoqueCentral.setQuantidadeAtual(
			estoqueCentral.getQuantidadeAtual() - dto.getQuantidadeMovimentada());
			}
			
			movimentacao.setDataMovimentacao(LocalDateTime.now());
			
			movimentacao.setObservacao(dto.getObservacao());
			}
    
    private void validarQuantidade(MovimentacaoEstoqueDTO dto) {
        if (dto.getQuantidadeMovimentada() == null ||
            dto.getQuantidadeMovimentada() <= 0) {

            throw new IllegalArgumentException(
                    "Quantidade movimentada inválida.");
        }
    }
}
