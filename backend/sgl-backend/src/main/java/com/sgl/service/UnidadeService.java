package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
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
    public UnidadeResponseDTO criar(UnidadeRequestDTO dto) {
        if (unidadeRepository.existsBySigla(dto.getSigla())) {
            throw new BusinessRuleException("Já existe uma unidade com esta sigla.");
        }

        Unidade unidade = new Unidade();
        unidade.setNome(dto.getNome());
        unidade.setSigla(dto.getSigla());
        return new UnidadeResponseDTO(unidadeRepository.save(unidade));
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponseDTO> listarTodos() {
        return unidadeRepository.findAll().stream()
                .map(UnidadeResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnidadeResponseDTO buscarPorId(UUID id) {
        Unidade unidade = unidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
        return new UnidadeResponseDTO(unidade);
    }

    @Transactional
    public UnidadeResponseDTO atualizar(UUID id, UnidadeRequestDTO dto) {
        Unidade unidade = unidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));

        if (unidadeRepository.existsBySiglaAndIdNot(dto.getSigla(), unidade.getId())) {
            throw new BusinessRuleException("Já existe uma unidade com esta sigla.");
        }

        unidade.setNome(dto.getNome());
        unidade.setSigla(dto.getSigla());
        return new UnidadeResponseDTO(unidadeRepository.save(unidade));
    }

    @Transactional
    public void deletar(UUID id) {
        Unidade unidade = unidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
        unidadeRepository.delete(unidade);
    }
}
