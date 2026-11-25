package com.sgl.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.request.BolsaRequestDTO;
import com.sgl.dto.response.BolsaResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Bolsa;
import com.sgl.model.Unidade;
import com.sgl.model.enums.TipoBolsa;
import com.sgl.repository.BolsaRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BolsaService {

    private final BolsaRepository repository;
    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public List<BolsaResponseDTO> listarTodas() {
        UUID unidadeId = exigirUnidade();
        return repository.findByUnidadePublicIdOrderByNomeAsc(unidadeId).stream()
                .map(BolsaResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<BolsaResponseDTO> listarAtivas() {
        UUID unidadeId = exigirUnidade();
        return repository.findByUnidadePublicIdAndAtivoTrueOrderByNomeAsc(unidadeId).stream()
                .map(BolsaResponseDTO::new).toList();
    }

    @Transactional
    public BolsaResponseDTO criar(BolsaRequestDTO dto) {
        UUID unidadeId = exigirUnidade();
        Unidade unidade = unidadeRepository.findByPublicId(unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade", unidadeId));
        String nome = normalizarNome(dto.getNome());
        validarUnicidade(unidade.getId(), nome, null);

        Bolsa bolsa = Bolsa.builder()
                .unidade(unidade)
                .nome(nome)
                .ativo(dto.getAtivo() == null || dto.getAtivo())
                .build();
        return new BolsaResponseDTO(repository.save(bolsa));
    }

    @Transactional
    public BolsaResponseDTO atualizar(UUID id, BolsaRequestDTO dto) {
        Bolsa bolsa = buscarNoTenant(id);
        String nome = normalizarNome(dto.getNome());
        validarUnicidade(bolsa.getUnidade().getId(), nome, bolsa.getId());
        bolsa.setNome(nome);
        if (dto.getAtivo() != null) bolsa.setAtivo(dto.getAtivo());
        return new BolsaResponseDTO(repository.save(bolsa));
    }

    @Transactional
    public void inativar(UUID id) {
        Bolsa bolsa = buscarNoTenant(id);
        // Nunca remover fisicamente: vinculos antigos mantem referencia historica.
        bolsa.setAtivo(false);
    }

    /**
     * Um vínculo usa OU o contrato legado TipoBolsa OU uma bolsa do catálogo.
     * Nenhuma classificação artificial ou herança de enum é exigida.
     */
    @Transactional(readOnly = true)
    public Bolsa validarEscolha(TipoBolsa tipoLegado, UUID bolsaId) {
        if ((tipoLegado == null) == (bolsaId == null)) {
            throw new BusinessRuleException(
                    "Selecione um tipo de vínculo existente ou uma bolsa cadastrada, não ambos.");
        }
        return bolsaId == null ? null : validarParaVinculo(bolsaId);
    }

    /** Apenas bolsas ativas da mesma Unidade podem iniciar um novo vínculo. */
    @Transactional(readOnly = true)
    public Bolsa validarParaVinculo(UUID bolsaId) {
        if (bolsaId == null) return null;
        Bolsa bolsa = buscarNoTenant(bolsaId);
        if (!Boolean.TRUE.equals(bolsa.getAtivo())) {
            throw new BusinessRuleException("A bolsa selecionada está inativa.");
        }
        return bolsa;
    }

    private Bolsa buscarNoTenant(UUID id) {
        UUID unidadeId = exigirUnidade();
        return repository.findByPublicIdAndUnidadePublicId(id, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Bolsa", id));
    }

    private UUID exigirUnidade() {
        return TenantContext.unidadeAtual()
                .orElseThrow(() -> new BusinessRuleException("Unidade em operação é obrigatória."));
    }

    private String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome da bolsa é obrigatório.");
        }
        String normalizado = nome.trim().replaceAll("\\s+", " ");
        if (normalizado.length() > 150) {
            throw new BusinessRuleException("Nome da bolsa excede 150 caracteres.");
        }
        return normalizado;
    }

    private void validarUnicidade(Long unidadeId, String nome, Long atualId) {
        boolean existe = atualId == null
                ? repository.existsByUnidadeIdAndNomeIgnoreCase(unidadeId, nome)
                : repository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidadeId, nome, atualId);
        if (existe) {
            throw new BusinessRuleException("Já existe uma bolsa com este nome na Unidade.");
        }
    }
}
