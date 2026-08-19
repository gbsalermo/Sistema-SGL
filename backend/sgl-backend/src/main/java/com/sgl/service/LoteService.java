package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.AtualizarLoteDTO;
import com.sgl.dto.LoteDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.EstoqueCentral;
import com.sgl.model.Lote;
import com.sgl.repository.EstoqueCentralRepository;
import com.sgl.repository.LoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoteService {

    private final LoteRepository loteRepository;
    private final EstoqueCentralRepository estoqueCentralRepository;

    @Transactional(readOnly = true)
    public List<LoteDTO> listarTodos() {
        return loteRepository.findAll()
                .stream()
                .map(LoteDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteDTO buscarPorId(UUID id) {
        Lote lote = loteRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));

        return new LoteDTO(lote);
    }

    @Transactional(readOnly = true)
    public List<LoteDTO> listarPorEstoque(UUID estoqueId) {
        EstoqueCentral estoque = estoqueCentralRepository.findByPublicId(estoqueId)
                .orElseThrow(() -> new ResourceNotFoundException("Estoque central", estoqueId));

        return loteRepository.findByEstoqueCentralId(estoque.getId())
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
    public LoteDTO atualizar(UUID id, AtualizarLoteDTO dto) {
        Lote lote = loteRepository.findByPublicId(id)
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

        lote.getEstoqueCentral()
                .getProduto()
                .validateLotExpirationDate(dto.getDataValidade());

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
    public void inativar(UUID id) {
        Lote lote = loteRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", id));

        if (lote.getQuantidadeDisponivel() > 0) {
            throw new BusinessRuleException(
                    "Lote com saldo disponível não pode ser inativado diretamente."
            );
        }

        lote.setAtivo(false);
    }
}
