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
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public LaboratorioResponseDTO criar(LaboratorioRequestDTO dto) {
        validarTenantUnidade(dto.getUnidadeId());

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
        // Correção de segurança: sem tenant ativo, este método devolvia os
        // laboratórios de TODAS as unidades (findAll). Agora exigimos o
        // header X-SGL-Unidade-Id também para listar.
        exigirTenantAtivo();

        List<Laboratorio> laboratorios = laboratorioRepository
                .findByUnidadePublicId(TenantContext.unidadeAtual().orElseThrow());

        return laboratorios.stream()
                .map(LaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LaboratorioResponseDTO> listarPorUnidade(UUID unidadeId) {
        validarTenantUnidade(unidadeId);
        return laboratorioRepository.findByUnidadePublicId(unidadeId).stream()
                .map(LaboratorioResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public LaboratorioResponseDTO buscarPorId(UUID id) {
        return new LaboratorioResponseDTO(buscarLaboratorioNoTenant(id));
    }

    @Transactional
    public LaboratorioResponseDTO atualizar(UUID id, LaboratorioRequestDTO dto) {
        Laboratorio laboratorio = buscarLaboratorioNoTenant(id);
        validarTenantUnidade(dto.getUnidadeId());

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
        Laboratorio laboratorio = buscarLaboratorioNoTenant(id);
        laboratorio.setAtivo(false);
        laboratorioRepository.save(laboratorio);
    }

    private Laboratorio buscarLaboratorioNoTenant(UUID id) {
        // Correção de segurança: antes, sem tenant ativo, buscava sem
        // filtro de unidade (findByPublicId), vazando dados de outra
        // unidade para quem não enviasse o header.
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        return laboratorioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
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

    private Usuario buscarResponsavelCompativel(UUID responsavelId, Unidade unidade) {
        exigirTenantAtivo();

        UUID unidadeId = TenantContext.unidadeAtual().orElseThrow();
        Usuario responsavel = usuarioRepository.findByPublicIdAndUnidadePublicId(responsavelId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável", responsavelId));

        if (responsavel.getUnidade() == null
                || !responsavel.getUnidade().getId().equals(unidade.getId())) {
            throw new BusinessRuleException(
                    "O responsável deve pertencer à mesma unidade do laboratório."
            );
        }

        return responsavel;
    }
}
