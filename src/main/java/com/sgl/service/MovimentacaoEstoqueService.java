package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.MovimentacaoEstoqueDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.enums.TipoMovimentacao;
import com.sgl.repository.MovimentacaoEstoqueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoRepository;

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
}
