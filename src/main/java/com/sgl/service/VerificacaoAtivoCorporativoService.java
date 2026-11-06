package com.sgl.service;

import org.springframework.stereotype.Service;

import com.sgl.corporate.CorporateApiClient;
import com.sgl.corporate.CorporateApiUnavailableException;
import com.sgl.exception.BusinessRuleException;
import com.sgl.model.Usuario;

import lombok.RequiredArgsConstructor;

/**
 * Confirms with the corporate base that the intern and the orientador of a new internship link are
 * still active, since the SGL copy only learns about departures at the person's next login.
 *
 * Deliberately not @Transactional: a refusal thrown from a transactional proxy would mark the caller's
 * transaction rollback-only, and the deactivation is committed separately by DesativacaoCorporativaService.
 */
@Service
@RequiredArgsConstructor
public class VerificacaoAtivoCorporativoService {

    private static final String MENSAGEM_INDISPONIVEL = "Não foi possível confirmar na base corporativa se o "
            + "estagiário e o orientador estão ativos. Tente novamente.";

    private final CorporateApiClient client;
    private final DesativacaoCorporativaService desativacao;

    public void exigirAtivos(Usuario estagiario, Usuario orientador) {
        // Dev/demo run without corporate credentials; there is nothing to confirm against.
        if (!client.isEnabled()) {
            return;
        }
        exigirAtivo(estagiario, "estagiário");
        exigirAtivo(orientador, "orientador");
    }

    private void exigirAtivo(Usuario pessoa, String papel) {
        if (!ativoNaBaseCorporativa(pessoa, papel)) {
            desativacao.desativar(pessoa);
            throw new BusinessRuleException(
                    "O %s %s está inativo na base corporativa.".formatted(papel, pessoa.getNome()));
        }
    }

    // Matrícula identifies an Empregado; without it, the CPF identifies a Colaborador. Not found counts as inactive.
    private boolean ativoNaBaseCorporativa(Usuario pessoa, String papel) {
        try {
            if (!isBlank(pessoa.getMatricula())) {
                return client.empregadoPorMatricula(pessoa.getMatricula())
                        .map(empregado -> Boolean.TRUE.equals(empregado.ativo()))
                        .orElse(false);
            }
            if (!isBlank(pessoa.getCpf())) {
                return client.colaboradorPorCpf(pessoa.getCpf())
                        .map(colaborador -> Boolean.TRUE.equals(colaborador.ativo()))
                        .orElse(false);
            }
        } catch (CorporateApiUnavailableException e) {
            throw new BusinessRuleException(MENSAGEM_INDISPONIVEL);
        }
        throw new BusinessRuleException("Não é possível conferir na base corporativa o %s %s: cadastro sem matrícula e sem CPF."
                .formatted(papel, pessoa.getNome()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
