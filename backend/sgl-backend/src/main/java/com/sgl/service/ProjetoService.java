package com.sgl.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.ProjetoDTO;
import com.sgl.exception.BusinessRuleException;
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
    public ProjetoDTO buscarPorId(Long id) {
        return projetoRepository.findById(id)
                .map(ProjetoDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> listarPorLaboratorio(Long laboratorioId) {
        if (!laboratorioRepository.existsById(laboratorioId)) {
            throw new ResourceNotFoundException("Laboratório", laboratorioId);
        }

        return projetoRepository.findByLaboratorioId(laboratorioId)
                .stream().map(ProjetoDTO::new).toList();
    }

    @Transactional
    public ProjetoDTO atualizar(Long id, ProjetoDTO dto) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));

        Laboratorio novoLaboratorio = buscarLaboratorio(dto.getLaboratorioId());

        projeto.setLaboratorio(novoLaboratorio);
        preencherProjeto(projeto, dto);
        return new ProjetoDTO(projetoRepository.save(projeto));
    }

    @Transactional
    public void deletar(Long id) {
        Projeto projeto = projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
        projeto.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public List<ProjetoDTO> listarAtivos() {
        return projetoRepository.findByAtivoTrue().stream().map(ProjetoDTO::new).toList();
    }

    private void preencherProjeto(Projeto projeto, ProjetoDTO dto) {
        validarDatas(dto.getDataInicio(), dto.getDataFim());
        projeto.setNome(dto.getNome());
        projeto.setDescricao(dto.getDescricao());
        projeto.setDataInicio(dto.getDataInicio());
        projeto.setDataFim(dto.getDataFim());
        projeto.setResponsavel(dto.getResponsavel());

        if (projeto.getId() == null) {
            projeto.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        } else if (dto.getAtivo() != null) {
            projeto.setAtivo(dto.getAtivo());
        }
    }

    private Laboratorio buscarLaboratorio(Long laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository.findById(laboratorioId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        if (!Boolean.TRUE.equals(laboratorio.getAtivo())) {
            throw new BusinessRuleException("O laboratório informado está inativo.");
        }

        return laboratorio;
    }

    private void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim != null) {
            throw new BusinessRuleException("A data de início é obrigatória quando a data de fim for informada.");
        }
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new BusinessRuleException("A data de início não pode ser posterior à data de fim.");
        }
    }
}
