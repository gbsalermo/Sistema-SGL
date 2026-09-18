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
        // Correção de segurança: sem tenant ativo, caía num "findAll" que
        // devolvia projetos de todas as unidades. Agora o header
        // X-SGL-Unidade-Id é exigido também para listar.
        exigirTenantAtivo();

        List<Projeto> projetos = projetoRepository
                .findByLaboratorioUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());
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
        exigirTenantAtivo();

        List<Projeto> projetos = projetoRepository
                .findByLaboratorioUnidadePublicIdAndAtivoTrue(TenantContext.unidadeAtual().orElseThrow());
        return projetos.stream().map(ProjetoResponseDTO::new).toList();
    }

    private Projeto buscarProjetoNoTenant(UUID id) {
        // Correção de segurança: antes, sem tenant ativo, buscava sem
        // filtro de unidade (findByPublicId), vazando o projeto de outra
        // unidade para quem não enviasse o header.
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return projetoRepository.findByPublicIdAndLaboratorioUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
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
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        Laboratorio laboratorio = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(laboratorioId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));

        laboratorio.validateActive();
        return laboratorio;
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    /**
     * Garante que existe uma unidade (tenant) definida para a requisição
     * atual. Ver o mesmo método em EstoqueCentralService para a explicação
     * completa do porquê essa checagem existe.
     */
    private void exigirTenantAtivo() {
        if (!TenantContext.ativo()) {
            throw new BusinessRuleException(
                    "Cabeçalho X-SGL-Unidade-Id é obrigatório para esta operação.");
        }
    }
}
