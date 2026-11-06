package com.sgl.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Unidade;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.security.PerfilPolicy;

import lombok.RequiredArgsConstructor;

/**
 * Single validator for every write path of a person's laboratory list (import, approval, user
 * create/edit, Gestão's "definir laboratórios"), so the rules cannot drift between them.
 */
@Component
@RequiredArgsConstructor
public class LaboratoriosDaPessoa {

    private final LaboratorioRepository laboratorioRepository;

    /**
     * @param atuais laboratories the person already has; an inactive one among them may be kept,
     *               but an inactive one cannot be added
     * @return the resolved laboratories, in the order received
     */
    public Set<Laboratorio> resolver(List<UUID> laboratorioIds, Unidade unidade, Perfil perfil,
            Set<Laboratorio> atuais) {
        List<UUID> ids = laboratorioIds == null ? List.of() : laboratorioIds;
        // Gestão oversees the Unidade and may have no laboratory; operational profiles need one.
        if (ids.isEmpty() && !PerfilPolicy.isGestao(perfil)) {
            throw new BusinessRuleException("Informe ao menos um laboratório.");
        }
        Set<UUID> vistos = new LinkedHashSet<>();
        Set<Laboratorio> resolvidos = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (!vistos.add(id)) {
                throw new BusinessRuleException("Laboratório repetido na lista.");
            }
            Laboratorio laboratorio = laboratorioRepository
                    .findByPublicIdAndUnidadePublicId(id, unidade.getPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Laboratório", id));
            boolean jaAtuava = atuais != null && atuais.stream()
                    .anyMatch(a -> a.getPublicId().equals(laboratorio.getPublicId()));
            if (!Boolean.TRUE.equals(laboratorio.getAtivo()) && !jaAtuava) {
                throw new BusinessRuleException("O laboratório " + laboratorio.getNome() + " está inativo.");
            }
            resolvidos.add(laboratorio);
        }
        return resolvidos;
    }
}
