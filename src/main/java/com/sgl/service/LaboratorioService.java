package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.LaboratorioRequestDTO;
import com.sgl.dto.response.LaboratorioResponseDTO;
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
    public LaboratorioResponseDTO criar(LaboratorioRequestDTO dto) {
        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setDescricao(dto.getDescricao());

        if (dto.getResponsavelId() != null) {
            Usuario responsavel = buscarResponsavelCompativel(dto.getResponsavelId(), unidade);
            laboratorio.setResponsavel(responsavel);
        }

        laboratorio.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        laboratorio.setNome(dto.getNome());
        laboratorio.setUnidade(unidade);

        return new LaboratorioResponseDTO(laboratorioRepository.save(laboratorio));
    }

    @Transactional(readOnly = true)
    public List<LaboratorioResponseDTO> listarTodos() {
        return laboratorioRepository.findAll().stream()
                .map(LaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LaboratorioResponseDTO> listarPorUnidade(UUID unidadeId) {
        Unidade unidade = unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));

        return laboratorioRepository.findByUnidadeId(unidade.getId()).stream()
                .map(LaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LaboratorioResponseDTO buscarPorId(UUID id) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
        return new LaboratorioResponseDTO(laboratorio);
    }

    @Transactional
    public LaboratorioResponseDTO atualizar(UUID id, LaboratorioRequestDTO dto) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));

        Unidade unidade = unidadeRepository.findByPublicId(dto.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", dto.getUnidadeId()));

        if (dto.getResponsavelId() != null) {
            laboratorio.setResponsavel(buscarResponsavelCompativel(dto.getResponsavelId(), unidade));
        } else {
            laboratorio.setResponsavel(null);
        }

        laboratorio.setDescricao(dto.getDescricao());
        laboratorio.setNome(dto.getNome());
        laboratorio.setUnidade(unidade);

        if (dto.getAtivo() != null) {
            laboratorio.setAtivo(dto.getAtivo());
        }

        return new LaboratorioResponseDTO(laboratorioRepository.save(laboratorio));
    }

    @Transactional
    public void deletar(UUID id) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));

        laboratorio.setAtivo(false);
        laboratorioRepository.save(laboratorio);
    }

    private Usuario buscarResponsavelCompativel(UUID responsavelId, Unidade unidade) {
        Usuario responsavel = usuarioRepository.findByPublicId(responsavelId)
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
