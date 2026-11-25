package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.ModalidadeBolsaRequestDTO;
import com.sgl.dto.response.ModalidadeBolsaResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.ModalidadeBolsa;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.ModalidadeBolsaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModalidadeBolsaService {

    private final ModalidadeBolsaRepository repository;
    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public List<ModalidadeBolsaResponseDTO> listarTodas() {
        UUID unidadeId = exigirUnidade();
        return repository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream()
                .map(ModalidadeBolsaResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<ModalidadeBolsaResponseDTO> listarAtivas() {
        UUID unidadeId = exigirUnidade();
        return repository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
                .map(ModalidadeBolsaResponseDTO::new).toList();
    }

    @Transactional
    public ModalidadeBolsaResponseDTO criar(ModalidadeBolsaRequestDTO dto) {
        UUID unidadeId = exigirUnidade();
        Unidade unidade = unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
        String nome = normalizarNome(dto.getNome());
        validarUnicidade(unidade.getId(), nome, null);

        ModalidadeBolsa modalidade = ModalidadeBolsa.builder()
                .unidade(unidade)
                .nome(nome)
                .tipoBase(dto.getTipoBase())
                .ativo(dto.getAtivo() == null || dto.getAtivo())
                .build();
        return new ModalidadeBolsaResponseDTO(repository.save(modalidade));
    }

    @Transactional
    public ModalidadeBolsaResponseDTO atualizar(UUID id, ModalidadeBolsaRequestDTO dto) {
        ModalidadeBolsa modalidade = buscarNoTenant(id);
        if (modalidade.getTipoBase() != dto.getTipoBase()) {
            throw new BusinessRuleException("O tipo base não pode ser alterado. Cadastre outra modalidade.");
        }
        String nome = normalizarNome(dto.getNome());
        validarUnicidade(modalidade.getUnidade().getId(), nome, modalidade.getId());
        modalidade.setNome(nome);
        if (dto.getAtivo() != null) modalidade.setAtivo(dto.getAtivo());
        return new ModalidadeBolsaResponseDTO(repository.save(modalidade));
    }

    @Transactional
    public void inativar(UUID id) {
        ModalidadeBolsa modalidade = buscarNoTenant(id);
        // Nunca remover fisicamente: vinculos antigos mantem referencia historica.
        modalidade.setAtivo(false);
    }

    /** Restringe selecao a modalidade ativa da Unidade e ao enum legado informado. */
    @Transactional(readOnly = true)
    public ModalidadeBolsa validarParaVinculo(UUID modalidadeId, TipoBolsa tipoBase) {
        if (modalidadeId == null) return null;
        ModalidadeBolsa modalidade = buscarNoTenant(modalidadeId);
        if (!Boolean.TRUE.equals(modalidade.getAtivo())) {
            throw new BusinessRuleException("A modalidade de bolsa está inativa.");
        }
        if (modalidade.getTipoBase() != tipoBase) {
            throw new BusinessRuleException("A modalidade não corresponde ao tipo base selecionado.");
        }
        return modalidade;
    }

    private ModalidadeBolsa buscarNoTenant(UUID id) {
        UUID unidadeId = exigirUnidade();
        return repository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Modalidade de bolsa", id));
    }

    private UUID exigirUnidade() {
        return TenantContext.unidadeAtual()
                .orElseThrow(() -> new BusinessRuleException("Unidade em operação é obrigatória."));
    }

    private String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome da modalidade é obrigatório.");
        }
        String normalizado = nome.trim().replaceAll("\\s+", " ");
        if (normalizado.length() > 150) {
            throw new BusinessRuleException("Nome da modalidade excede 150 caracteres.");
        }
        return normalizado;
    }

    private void validarUnicidade(Long unidadeId, String nome, Long atualId) {
        boolean existe = atualId == null
                ? repository.existsByUnidadeIdAndNomeIgnoreCase(unidadeId, nome)
                : repository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidadeId, nome, atualId);
        if (existe) {
            throw new BusinessRuleException("Já existe uma modalidade com este nome na Unidade.");
        }
    }
}
