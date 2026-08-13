package com.sgl.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.LaboratorioDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.Usuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public LaboratorioDTO criar(LaboratorioDTO dto) {
        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setDescricao(dto.getDescricao());

        if (dto.getResponsavel() != null) {
            Usuario responsavel = buscarResponsavelCompativel(dto.getResponsavel(), unidade);
            laboratorio.setResponsavel(responsavel);
        }

        laboratorio.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        laboratorio.setNome(dto.getNome());
        laboratorio.setUnidade(unidade);

        return new LaboratorioDTO(laboratorioRepository.save(laboratorio));
    }

    @Transactional(readOnly = true)
    public List<LaboratorioDTO> listarTodos() {
        return laboratorioRepository.findAll().stream()
                .map(LaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LaboratorioDTO> listarPorUnidade(Long unidadeId) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new ResourceNotFoundException("Unidade", unidadeId);
        }

        return laboratorioRepository.findByUnidadeId(unidadeId).stream()
                .map(LaboratorioDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LaboratorioDTO buscarPorId(Long id) {
        Laboratorio laboratorio = laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
        return new LaboratorioDTO(laboratorio);
    }

    @Transactional
    public LaboratorioDTO atualizar(Long id, LaboratorioDTO dto) {
        Laboratorio laboratorio = laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));

        Unidade unidade = unidadeRepository.findById(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        if (dto.getResponsavel() != null) {
            laboratorio.setResponsavel(buscarResponsavelCompativel(dto.getResponsavel(), unidade));
        } else {
            laboratorio.setResponsavel(null);
        }

        laboratorio.setDescricao(dto.getDescricao());
        laboratorio.setNome(dto.getNome());
        laboratorio.setUnidade(unidade);

        if (dto.getAtivo() != null) {
            laboratorio.setAtivo(dto.getAtivo());
        }

        return new LaboratorioDTO(laboratorioRepository.save(laboratorio));
    }

    @Transactional
    public void deletar(Long id) {
        Laboratorio laboratorio = laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));

        laboratorio.setAtivo(false);
        laboratorioRepository.save(laboratorio);
    }

    private Usuario buscarResponsavelCompativel(Long responsavelId, Unidade unidade) {
        Usuario responsavel = usuarioRepository.findById(responsavelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuário responsável",
                        responsavelId
                ));

        if (responsavel.getUnidade() == null
                || !responsavel.getUnidade().getId().equals(unidade.getId())) {
            throw new BusinessRuleException(
                    "O responsável deve pertencer à mesma unidade do laboratório."
            );
        }

        return responsavel;
    }
}
