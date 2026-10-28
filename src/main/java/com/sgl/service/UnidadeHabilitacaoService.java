package com.sgl.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.corporate.CorporateApiClient;
import com.sgl.corporate.CorporateDtos.UnidadeCorporativa;
import com.sgl.dto.response.UnidadeCorporativaResponseDTO;
import com.sgl.dto.response.UnidadeResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.exception.UnidadeRejectedException;
import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PerfilPolicy;

import lombok.RequiredArgsConstructor;

/** Lets the general administrator bring Unidades from the corporate list into the SGL (spec 2.4). */
@Service
@RequiredArgsConstructor
public class UnidadeHabilitacaoService {

    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");

    private final CorporateApiClient corporateApiClient;
    private final UnidadeRepository unidadeRepository;

    @Transactional(readOnly = true)
    public List<UnidadeCorporativaResponseDTO> listarCorporativas() {
        exigirAdministradorGeral(CurrentUser.get());
        Set<Integer> habilitadas = unidadeRepository.findAllSemFiltroDeTenant().stream()
                .map(Unidade::getCodigoCorporativo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return corporateApiClient.unidadesEmbrapa().stream()
                .map(u -> new UnidadeCorporativaResponseDTO(u.unidadeId(), u.sigla(), u.nome(),
                        habilitadas.contains(u.unidadeId())))
                .toList();
    }

    @Transactional
    public UnidadeResponseDTO habilitar(Integer codigoCorporativo) {
        AuthenticatedUser ator = CurrentUser.get();
        exigirAdministradorGeral(ator);
        if (unidadeRepository.findByCodigoCorporativo(codigoCorporativo).isPresent()) {
            throw UnidadeRejectedException.jaHabilitada();
        }
        // Name and sigla come from the corporate list, never from the browser.
        UnidadeCorporativa corporativa = corporateApiClient.unidadesEmbrapa().stream()
                .filter(u -> codigoCorporativo.equals(u.unidadeId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Unidade corporativa", codigoCorporativo));
        if (unidadeRepository.existsBySigla(corporativa.sigla())) {
            throw UnidadeRejectedException.siglaEmUso(corporativa.sigla());
        }

        Unidade salva = unidadeRepository.save(Unidade.builder()
                .nome(corporativa.nome())
                .sigla(corporativa.sigla())
                .codigoCorporativo(codigoCorporativo)
                .build());
        AUDIT.info("acao=UNIDADE_HABILITADA ator={} alvo={} codigoCorporativo={}",
                ator.publicId(), salva.getPublicId(), codigoCorporativo);
        return new UnidadeResponseDTO(salva);
    }

    private static void exigirAdministradorGeral(AuthenticatedUser ator) {
        if (!PerfilPolicy.isAdministradorGeral(ator.perfil())) {
            throw new AccessDeniedException("Apenas o Administrador geral pode habilitar Unidades.");
        }
    }
}
