package com.sgl.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.MembroEquipeResponseDTO;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.exception.UsuarioRejectedException;
import com.sgl.model.Laboratorio;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoOperacionalUsuario;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.LaboratorioRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoOperacionalUsuarioRepository;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.CurrentUser;
import com.sgl.security.PapelPolicy;

import lombok.RequiredArgsConstructor;

/**
 * The team of one laboratory: the people who have it in their list, plus those with a running
 * temporary link to it. Managed by the laboratory's responsável or by an administrator of the
 * Unidade; a GESTOR alone does not manage teams.
 */
@Service
@RequiredArgsConstructor
public class EquipeLaboratorioService {

    private static final Logger AUDIT = LoggerFactory.getLogger("sgl.auditoria");

    private final LaboratorioRepository laboratorioRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoOperacionalUsuarioRepository vinculoRepository;
    private final Clock clock;

    /**
     * Loads the laboratory of the Unidade in operation (404 otherwise) and refuses (403) anyone who is
     * neither an administrator nor the responsável of this active laboratory.
     */
    @Transactional(readOnly = true)
    public Laboratorio exigirGestaoDaEquipe(UUID laboratorioId) {
        Laboratorio laboratorio = laboratorioRepository
                .findByPublicIdAndUnidadePublicId(laboratorioId, CurrentUser.unidadeEmOperacao())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
        AuthenticatedUser ator = CurrentUser.get();
        if (PapelPolicy.isAdministrador(ator.papeis())) {
            return laboratorio;
        }
        boolean responsavel = laboratorio.getResponsavel() != null
                && ator.publicId().equals(laboratorio.getResponsavel().getPublicId());
        if (!responsavel || !Boolean.TRUE.equals(laboratorio.getAtivo())) {
            throw new AccessDeniedException("Só o responsável pelo laboratório ou um administrador organiza esta equipe.");
        }
        return laboratorio;
    }

    @Transactional(readOnly = true)
    public List<MembroEquipeResponseDTO> listar(UUID laboratorioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        LocalDate hoje = LocalDate.now(clock);
        Map<Long, Usuario> pessoas = new LinkedHashMap<>();
        usuarioRepository.findByLaboratoriosId(laboratorio.getId()).forEach(u -> pessoas.put(u.getId(), u));
        Map<Long, List<VinculoOperacionalUsuario>> vinculosPorPessoa = new LinkedHashMap<>();
        for (VinculoOperacionalUsuario vinculo : vinculoRepository.findByLaboratorioAndEncerradoFalse(laboratorio)) {
            if (vinculo.vigente(hoje)) {
                pessoas.putIfAbsent(vinculo.getUsuario().getId(), vinculo.getUsuario());
                vinculosPorPessoa.computeIfAbsent(vinculo.getUsuario().getId(), id -> new ArrayList<>()).add(vinculo);
            }
        }
        Long responsavelId = laboratorio.getResponsavel() != null ? laboratorio.getResponsavel().getId() : null;
        return pessoas.values().stream()
                .map(u -> membro(u, laboratorio, vinculosPorPessoa.getOrDefault(u.getId(), List.of()), responsavelId))
                .sorted(Comparator.comparing(MembroEquipeResponseDTO::responsavel).reversed()
                        .thenComparing(MembroEquipeResponseDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public void incluir(UUID laboratorioId, UUID usuarioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        Usuario alvo = alvoDaUnidade(usuarioId);
        if (alvo.getSituacao() == SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.pendenteExigeAprovacao();
        }
        alvo.validateActive();
        if (alvo.atuaNoLaboratorio(laboratorio)) {
            return;
        }
        List<Laboratorio> novos = new ArrayList<>(alvo.getLaboratorios());
        novos.add(laboratorio);
        alvo.substituirLaboratorios(novos);
        usuarioRepository.save(alvo);
        auditar("EQUIPE_INCLUSAO", alvo, laboratorio);
    }

    /**
     * Removing someone means they lose access to this laboratory: it leaves their list and their
     * running temporary links to it are closed today. Their other laboratories are untouched, and
     * removing the last one is allowed (the person stays active, waiting for another team).
     */
    @Transactional
    public void retirar(UUID laboratorioId, UUID usuarioId) {
        Laboratorio laboratorio = exigirGestaoDaEquipe(laboratorioId);
        Usuario alvo = alvoDaUnidade(usuarioId);
        if (laboratorio.getResponsavel() != null && alvo.getId().equals(laboratorio.getResponsavel().getId())) {
            throw UsuarioRejectedException.responsavelNaEquipe();
        }
        LocalDate hoje = LocalDate.now(clock);
        List<VinculoOperacionalUsuario> vinculos = vinculoRepository
                .findByLaboratorioAndUsuarioAndEncerradoFalse(laboratorio, alvo);
        boolean naLista = alvo.atuaNoLaboratorio(laboratorio);
        if (!naLista && vinculos.isEmpty()) {
            throw new ResourceNotFoundException("Pessoa na equipe do laboratório", usuarioId);
        }
        if (naLista) {
            alvo.substituirLaboratorios(alvo.getLaboratorios().stream()
                    .filter(item -> !Objects.equals(item.getId(), laboratorio.getId()))
                    .toList());
            usuarioRepository.save(alvo);
        }
        vinculos.forEach(vinculo -> {
            vinculo.setEncerrado(true);
            if (vinculo.getDataFim() == null || vinculo.getDataFim().isAfter(hoje)) {
                vinculo.setDataFim(hoje);
            }
        });
        vinculoRepository.saveAll(vinculos);
        auditar("EQUIPE_RETIRADA", alvo, laboratorio);
    }

    private Usuario alvoDaUnidade(UUID usuarioId) {
        if (CurrentUser.get().publicId().equals(usuarioId)) {
            throw new AccessDeniedException("Ninguém altera o próprio acesso à equipe.");
        }
        return usuarioRepository.findByPublicIdAndUnidadePublicId(usuarioId, CurrentUser.unidadeEmOperacao())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
    }

    private void auditar(String acao, Usuario alvo, Laboratorio laboratorio) {
        AUDIT.info("acao={} ator={} alvo={} laboratorio={}", acao, CurrentUser.get().publicId(),
                alvo.getPublicId(), laboratorio.getPublicId());
    }

    private static MembroEquipeResponseDTO membro(Usuario usuario, Laboratorio laboratorio,
            List<VinculoOperacionalUsuario> vinculos, Long responsavelId) {
        boolean semFim = vinculos.stream().anyMatch(v -> v.getDataFim() == null);
        LocalDate ate = semFim ? null : vinculos.stream()
                .map(VinculoOperacionalUsuario::getDataFim)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new MembroEquipeResponseDTO(usuario.getPublicId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPapeis(), usuario.getCategoriaCargo(), usuario.getCargo(),
                usuario.atuaNoLaboratorio(laboratorio), ate, usuario.getId().equals(responsavelId));
    }
}
