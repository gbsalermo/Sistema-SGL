package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.UnidadeRequestDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    @Transactional
    public UnidadeResponseDTO criar(UnidadeRequestDTO dto) {
        exigirAdministradorGeral();
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
        // The general administrator's Unidade selector needs them all, not just the one in operation.
        boolean administradorGeral = CurrentUser.find()
                .map(user -> PerfilPolicy.isAdministradorGeral(user.perfil()))
                .orElse(false);
        List<Unidade> unidades = administradorGeral
                ? unidadeRepository.findAllSemFiltroDeTenant()
                : unidadeRepository.findAll();
        return unidades.stream()
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
        exigirAdministradorGeral();
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
        exigirAdministradorGeral();
        Unidade unidade = unidadeRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", id));
        unidadeRepository.delete(unidade);
    }

    // Unidades span every tenant, so only the general administrator may create, rename or remove them.
    private static void exigirAdministradorGeral() {
        if (!PerfilPolicy.isAdministradorGeral(CurrentUser.get().perfil())) {
            throw new AccessDeniedException("Apenas o Administrador geral pode alterar o cadastro de Unidades.");
        }
    }
}
