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
        List<Laboratorio> laboratorios = TenantContext.unidadeAtual()
                .map(laboratorioRepository::findByUnidadePublicId)
                .orElseGet(laboratorioRepository::findAll);

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
        return TenantContext.unidadeAtual()
                .flatMap(unidadeId -> laboratorioRepository.findByPublicIdAndUnidadePublicId(id, unidadeId))
                .orElseGet(() -> {
                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException("Laboratório", id);
                    }
                    return laboratorioRepository.findByPublicId(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
                });
    }

    private void validarTenantUnidade(UUID unidadeId) {
        if (!TenantContext.pertence(unidadeId)) {
            throw new BusinessRuleException("A operação não pode acessar dados de outra unidade.");
        }
    }

    private Usuario buscarResponsavelCompativel(UUID responsavelId, Unidade unidade) {
        Usuario responsavel = TenantContext.unidadeAtual()
                .flatMap(unidadeId -> usuarioRepository.findByPublicIdAndUnidadePublicId(responsavelId, unidadeId))
                .orElseGet(() -> {
                    if (TenantContext.ativo()) {
                        throw new ResourceNotFoundException("Usuário responsável", responsavelId);
                    }
                    return usuarioRepository.findByPublicId(responsavelId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável", responsavelId));
                });

        if (responsavel.getUnidade() == null
                || !responsavel.getUnidade().getId().equals(unidade.getId())) {
            throw new BusinessRuleException(
                    "O responsável deve pertencer à mesma unidade do laboratório."
            );
        }

        return responsavel;
    }
}
