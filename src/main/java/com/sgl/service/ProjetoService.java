package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProjetoDTO;
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
    public ProjetoDTO criar(ProjetoDTO dto) {
        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());

        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .build();
        preencherProjeto(projeto, dto);

        Projeto salvo = projetoRepository.save(projeto);
        return new ProjetoDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> listarTodos() {
        return projetoRepository.findAll().stream().map(ProjetoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoDTO buscarPorId(UUID id) {
        return projetoRepository.findByPublicId(id)
                .map(ProjetoDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findByPublicId(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        return projetoRepository.findByLaboratorioId(laboratorio.getId())
                .stream().map(ProjetoDTO::new).toList();
    }

    @Transactional
    public ProjetoDTO atualizar(UUID id, ProjetoDTO dto) {
        Projeto projeto = projetoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));

        Laboratorio novoLaboratorio = buscarLaboratorio(dto.getLaboratorioId());

        projeto.setLaboratorio(novoLaboratorio);
        preencherProjeto(projeto, dto);
        return new ProjetoDTO(projetoRepository.save(projeto));
    }

    @Transactional
    public void deletar(UUID id) {
        Projeto projeto = projetoRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
        projeto.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> listarAtivos() {
        return projetoRepository.findByAtivoTrue().stream().map(ProjetoDTO::new).toList();
    }

    private void preencherProjeto(Projeto projeto, ProjetoDTO dto) {
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
