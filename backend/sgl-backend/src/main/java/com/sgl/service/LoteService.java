package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Lote;
import com.sgl.model.Produto;
import com.sgl.repository.LoteRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cuida de consultas e manutenção cadastral do lote.
 * Alterações de quantidade pertencem ao fluxo de movimentação física.
 */
@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;

    @Transactional(readOnly = true)
    public List<LoteDTO> listarTodos() {
        return loteRepository.findAll()
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteDTO buscarPorId(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));

        return new LoteDTO(lote);
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarPorEstoque(Long estoqueId) {
        return loteRepository.findByEstoqueCentralId(estoqueId)
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarVencidos() {
        return loteRepository
                .findByDataValidadeBeforeAndAtivoTrue(LocalDate.now())
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional
    public LoteDTO atualizar(Long id, AtualizarLoteDTO dto) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));

        boolean numeroDuplicado = loteRepository
                .existsByEstoqueCentralIdAndNumeroLote(
                        lote.getEstoqueCentral().getId(),
                        dto.getNumeroLote()
                ) && !lote.getNumeroLote().equals(dto.getNumeroLote());

        if (numeroDuplicado) {
            throw new BusinessRuleException(
                    "Já existe lote com esse número neste estoque."
            );
        }

        validarValidadeDoProduto(
                lote.getEstoqueCentral().getProduto(),
                dto.getDataValidade()
        );

        if (Boolean.FALSE.equals(dto.getAtivo())
                && lote.getQuantidadeDisponivel() > 0) {
            throw new BusinessRuleException(
                    "Lote com saldo disponível não pode ser inativado diretamente."
            );
        }

        lote.setNumeroLote(dto.getNumeroLote());
        lote.setDataValidade(dto.getDataValidade());

        if (dto.getAtivo() != null) {
            lote.setAtivo(dto.getAtivo());
        }

        return new LoteDTO(loteRepository.save(lote));
    }

    @Transactional
    public void inativar(Long id) {
        Lote lote = loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));

        if (lote.getQuantidadeDisponivel() > 0) {
            throw new BusinessRuleException(
                    "Lote com saldo disponível não pode ser inativado diretamente."
            );
        }

        lote.setAtivo(false);
    }

    private void validarValidadeDoProduto(Produto produto, LocalDate dataValidade) {
        if (Boolean.TRUE.equals(produto.getPerecivel()) && dataValidade == null) {
            throw new BusinessRuleException(
                    "Data de validade é obrigatória para produto perecível."
            );
        }

        if (!Boolean.TRUE.equals(produto.getPerecivel()) && dataValidade != null) {
            throw new BusinessRuleException(
                    "Produto não perecível não deve possuir data de validade no lote."
            );
        }
    }
}
