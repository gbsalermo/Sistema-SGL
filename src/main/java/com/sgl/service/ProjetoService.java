package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ProjetoRequestDTO;
import com.sgl.dto.response.ProjetoResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final LaboratorioRepository laboratorioRepository;

    @Transactional
    public ProjetoResponseDTO criar(ProjetoRequestDTO dto) {
        Laboratorio laboratorio = buscarLaboratorio(dto.getLaboratorioId());
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .build();
        preencherProjeto(projeto, dto);

        Projeto salvo = projetoRepository.save(projeto);
        return new ProjetoResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarTodos() {
        List<Projeto> projetos = TenantContext.unidadeAtual()
                .map(projetoRepository::findByLaboratorioUnidadePublicId)
                .orElseGet(projetoRepository::findAll);
        return projetos.stream().map(ProjetoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponseDTO buscarPorId(UUID id) {
        return new ProjetoResponseDTO(buscarProjetoNoTenant(id));
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarPorLaboratorio(UUID laboratorioId) {
        Laboratorio laboratorio = buscarLaboratorio(laboratorioId);
        validarTenantUnidade(laboratorio.getUnidade() != null ? laboratorio.getUnidade().getPublicId() : null);

        return projetoRepository.findByLaboratorioId(laboratorio.getId())
                .stream().map(ProjetoResponseDTO::new).toList();
    }

    @Transactional
    public ProjetoResponseDTO atualizar(UUID id, ProjetoRequestDTO dto) {
        Projeto projeto = buscarProjetoNoTenant(id);
        Laboratorio novoLaboratorio = buscarLaboratorio(dto.getLaboratorioId());
        validarTenantUnidade(novoLaboratorio.getUnidade() != null ? novoLaboratorio.getUnidade().getPublicId() : null);

        projeto.setLaboratorio(novoLaboratorio);
        preencherProjeto(projeto, dto);
        return new ProjetoResponseDTO(projetoRepository.save(projeto));
    }

    @Transactional
    public void deletar(UUID id) {
        Projeto projeto = buscarProjetoNoTenant(id);
        projeto.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponseDTO> listarAtivos() {
        List<Projeto> projetos = TenantContext.unidadeAtual()
                .map(projetoRepository::findByLaboratorioUnidadePublicIdAndAtivoTrue)
                .orElseGet(projetoRepository::findByAtivoTrue);
        return projetos.stream().map(ProjetoResponseDTO::new).toList();
    }

    private Projeto buscarProjetoNoTenant(UUID id) {
        return TenantContext.unidadeAtual()
                .flatMap(unidadeId -> projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId))
                .orElseGet(() -> {
                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException("Projeto", id);
                    }
                    return projetoRepository.findByPublicId(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
                });
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
        Laboratorio laboratorio = TenantContext.unidadeAtual()
                .flatMap(unidadeId -> laboratorioRepository.findByPublicIdAndUnidadePublicId(laboratorioId, unidadeId))
                .orElseGet(() -> {
                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException("Laboratório", laboratorioId);
                    }
                    return laboratorioRepository.findByPublicId(laboratorioId)
                            .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
                });

        laboratorio.validateActive();
        return laboratorio;
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }
}
