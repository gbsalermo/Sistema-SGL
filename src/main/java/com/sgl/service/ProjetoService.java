package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final LaboratorioRepository laboratorioRepository;

    @Transactional
    public ProjetoResponseDTO criar(ProjetoRequestDTO dto) {
        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .build();
        preencherProjeto(projeto, dto);

        Projeto salvo = projetoRepository.save(projeto);
        return new ProjetoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarTodos() {
        return projetoRepository.findAll().stream().map(ProjetoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponseDTO buscarPorId(UUID id) {
        return projetoRepository.findByPublicId(id)
                .map(ProjetoResponseDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        return projetoRepository.findByLaboratorioId(laboratorio.getId())
                .stream().map(ProjetoResponseDTO::new).toList();
    }

    @Transactional
    public ProjetoResponseDTO atualizar(UUID id, ProjetoRequestDTO dto) {
        Projeto projeto = projetoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));

        Laboratorio novoLaboratorio = buscarLaboratorio(dto.getLaboratorioId());

        projeto.setLaboratorio(novoLaboratorio);
        preencherProjeto(projeto, dto);
        return new ProjetoResponseDTO(projetoRepository.save(projeto));
    }

    @Transactional
    public void deletar(UUID id) {
        Projeto projeto = projetoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
        projeto.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarAtivos() {
        return projetoRepository.findByAtivoTrue().stream().map(ProjetoResponseDTO::new).toList();
    }

    private void preencherProjeto(Projeto projeto, ProjetoRequestDTO dto) {
        projeto.setNome(dto.getNome());
        projeto.setDescricao(dto.getDescricao());
        projeto.updateDates(dto.getDataInicio(), dto.getDataFim());
        projeto.setResponsavel(dto.getResponsavel());

        if (projeto.getId() == null) {
            projeto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        } else if (dto.getAtivo() != null) {
            projeto.setAtivo(dto.getAtivo());
        }
    }

    private Laboratorio buscarLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        laboratorio.validateActive();
        return laboratorio;
    }
}
