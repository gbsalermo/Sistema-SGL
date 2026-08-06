package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.UnidadeDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    @Transactional
    public UnidadeDTO criar(UnidadeDTO dto) {
        if (unidadeRepository.existsBySigla(dto.getSigla())) {
            throw new BusinessRuleException("Já existe uma unidade com esta sigla.");
        }

        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome());
        unidade.setSigla(dto.getSigla());
        return new UnidadeDTO(unidadeRepository.save(unidade));
    }

    @Transactional(readOnly = true)
    public List<UnidadeDTO> listarTodos() {
        return unidadeRepository.findAll().stream().map(UnidadeDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public UnidadeDTO buscarPorId(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
        return new UnidadeDTO(unidade);
    }

    @Transactional
    public UnidadeDTO atualizar(Long id, UnidadeDTO dto) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));

        if (unidadeRepository.existsBySiglaAndIdNot(dto.getSigla(), id)) {
            throw new BusinessRuleException("Já existe uma unidade com esta sigla.");
        }

        unidade.setNome(dto.getNome());
        unidade.setSigla(dto.getSigla());
        return new UnidadeDTO(unidadeRepository.save(unidade));
    }

    @Transactional
    public void deletar(Long id) {
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
        unidadeRepository.delete(unidade);
    }
}
