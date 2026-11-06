package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.dto.request.EditarVinculoOperacionalRequestDTO;
import com.sgl.dto.response.VinculoOperacionalUsuarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.exception.UsuarioRejectedException;
import com.sgl.model.*;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoUsuario;
import com.sgl.repository.*;
import com.sgl.security.CurrentUser;
import com.sgl.security.AuthenticatedUser;
import com.sgl.security.PerfilPolicy;
import com.sgl.tenant.TenantContext;

/** Contrato de domínio homologado em DEV; autorização de gestor depende do futuro SSO. */
@Service @RequiredArgsConstructor
public class VinculoOperacionalUsuarioService {
    private static final String VINCULO_GENERICO = "Use a lista de laboratórios da pessoa para acesso sem prazo; o vínculo adicional é para acesso temporário ou restrito a projeto, SCI ou atividade.";
    private final VinculoOperacionalUsuarioRepository vinculos;
    private final UsuarioRepository usuarios;
    private final LaboratorioRepository laboratorios;
    private final ProjetoRepository projetos;
    private final SciRepository scis;
    private final AtividadeRepository atividades;
    private final LaboratoriosDaPessoa laboratoriosDaPessoa;

    private UUID unidade() {
        return TenantContext.unidadeAtual().orElseThrow(
            () -> new BusinessRuleException("Unidade operacional obrigatória."));
    }

    /** Resumo de vínculos para a listagem administrativa, sem uma chamada por usuário. */
    public record ResumoUsuario(UUID usuarioId, int vinculosVigentes, int vinculosEncerrados,
            List<String> laboratoriosAdicionais) {}

    @Transactional(readOnly = true)
    public List<ResumoUsuario> resumo() {
        Map<UUID, List<VinculoOperacionalUsuario>> agrupados = new HashMap<>();
        for (VinculoOperacionalUsuario v : vinculos.findByUsuarioUnidadePublicId(unidade())) {
            agrupados.computeIfAbsent(v.getUsuario().getPublicId(), k -> new ArrayList<>()).add(v);
        }
        LocalDate hoje = LocalDate.now();
        return agrupados.entrySet().stream().map(e -> {
            var ativos = e.getValue().stream().filter(v -> v.vigente(hoje)).toList();
            var nomes = ativos.stream().map(v -> v.getLaboratorio().getNome())
                .distinct().sorted().toList();
            return new ResumoUsuario(e.getKey(), ativos.size(),
                (int) e.getValue().stream().filter(v -> !v.vigente(hoje)).count(), nomes);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<VinculoOperacionalUsuarioResponseDTO> listar(UUID usuarioId) {
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuarioId, unidade())
            .stream().map(VinculoOperacionalUsuarioResponseDTO::new).toList();
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO criar(VinculoOperacionalUsuarioRequestDTO dto) {
        exigirGestao();
        UUID tenant = unidade();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(dto.getUsuarioId(), tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));
        exigirAutoridadeSobre(usuario);
        if (usuario.getPerfil() == Perfil.ESTAGIARIO) {
            throw new BusinessRuleException("Estagiários usam vínculos de estágio e participações existentes.");
        }
        usuario.validateActive();
        Laboratorio laboratorio = laboratorios.findByPublicIdAndUnidadePublicId(dto.getLaboratorioId(), tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Laboratório", dto.getLaboratorioId()));
        laboratorio.validateActive();
        Projeto projeto = null;
        Sci sci = null;
        Atividade atividade = null;
        if (dto.getAtividadeId() != null) {
            atividade = atividades.findByPublicIdAndSciProjetoUnidadePublicId(dto.getAtividadeId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", dto.getAtividadeId()));
            atividade.validateActive();
            sci = atividade.getSci();
            projeto = sci.getProjeto();
        } else if (dto.getSciId() != null) {
            sci = scis.findByPublicIdAndProjetoUnidadePublicId(dto.getSciId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("SCI", dto.getSciId()));
            sci.validateActive();
            projeto = sci.getProjeto();
        } else if (dto.getProjetoId() != null) {
            projeto = projetos.findByPublicId(dto.getProjetoId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", dto.getProjetoId()));
        }
        if (sci != null) sci.validateActive();
        if (projeto != null) {
            projeto.validateActive();
            if (!projeto.pertenceAoLaboratorio(laboratorio)) {
                throw new BusinessRuleException("Projeto/SCI/Atividade não pertence ao laboratório informado.");
            }
        }
        if (dto.getProjetoId() != null && (projeto == null || !dto.getProjetoId().equals(projeto.getPublicId()))
            || dto.getSciId() != null && (sci == null || !dto.getSciId().equals(sci.getPublicId()))) {
            throw new BusinessRuleException("Contexto hierárquico divergente.");
        }
        // Access without a deadline belongs in the person's laboratory list; this link is only for
        // temporary access or access restricted to a projeto, SCI or atividade.
        if (projeto == null && sci == null && atividade == null && dto.getDataFim() == null) {
            throw new BusinessRuleException(VINCULO_GENERICO);
        }
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessRuleException("Data final anterior ao início.");
        }
        final Projeto projetoFinal = projeto;
        final Sci sciFinal = sci;
        final Atividade atividadeFinal = atividade;
        boolean repetido = vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), tenant)
            .stream().anyMatch(v -> !Boolean.TRUE.equals(v.getEncerrado()) && v.getDataFim() == null
                && v.getLaboratorio().getId().equals(laboratorio.getId())
                && java.util.Objects.equals(v.getProjeto() == null ? null : v.getProjeto().getId(), projetoFinal == null ? null : projetoFinal.getId())
                && java.util.Objects.equals(v.getSci() == null ? null : v.getSci().getId(), sciFinal == null ? null : sciFinal.getId())
                && java.util.Objects.equals(v.getAtividade() == null ? null : v.getAtividade().getId(), atividadeFinal == null ? null : atividadeFinal.getId()));
        if (repetido) throw new BusinessRuleException("Já existe vínculo aberto para este contexto.");
        VinculoOperacionalUsuario v = new VinculoOperacionalUsuario();
        v.setUsuario(usuario); v.setLaboratorio(laboratorio); v.setProjeto(projeto);
        v.setSci(sci); v.setAtividade(atividade);
        v.setDataInicio(dto.getDataInicio()); v.setDataFim(dto.getDataFim()); v.setObservacao(dto.getObservacao());
        return new VinculoOperacionalUsuarioResponseDTO(vinculos.save(v));
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO editar(UUID id, EditarVinculoOperacionalRequestDTO dto) {
        exigirGestao();
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        exigirAutoridadeSobre(v.getUsuario());
        if (Boolean.TRUE.equals(v.getEncerrado())) {
            throw new BusinessRuleException("Vínculo encerrado não pode ser editado.");
        }
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(v.getDataInicio())) {
            throw new BusinessRuleException("A data final não pode ser anterior à data de início.");
        }
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Para encerrar vínculo imediatamente, utilize Encerrar vínculo.");
        }
        // Clearing the deadline of a laboratory-wide link would recreate the generic link that
        // belongs in the person's laboratory list (same rule as criar).
        if (v.getProjeto() == null && v.getSci() == null && v.getAtividade() == null && dto.getDataFim() == null) {
            throw new BusinessRuleException(VINCULO_GENERICO);
        }
        v.setDataFim(dto.getDataFim());
        v.setObservacao(dto.getObservacao());
        return new VinculoOperacionalUsuarioResponseDTO(v);
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO encerrar(UUID id) {
        exigirGestao();
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        exigirAutoridadeSobre(v.getUsuario());
        if (Boolean.TRUE.equals(v.getEncerrado())) {
            throw new BusinessRuleException("Vínculo já encerrado.");
        }
        v.setDataFim(LocalDate.now());
        v.setEncerrado(true);
        return new VinculoOperacionalUsuarioResponseDTO(v);
    }

    @Transactional
    public void definirLaboratorios(UUID usuarioId, List<UUID> laboratorioIds) {
        exigirGestao();
        UUID tenant = unidade();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(usuarioId, tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
        exigirAutoridadeSobre(usuario);
        // Quem está pendente recebe os laboratórios na aprovação (aba Pendentes), não aqui:
        // salvar a lista sem aprovar deixaria a pessoa bloqueada e a gestão achando que liberou.
        if (usuario.getSituacao() == SituacaoUsuario.PENDENTE_VINCULACAO) {
            throw UsuarioRejectedException.pendenteExigeAprovacao();
        }
        usuario.substituirLaboratorios(laboratoriosDaPessoa.resolver(
            laboratorioIds, usuario.getUnidade(), usuario.getPerfil(), usuario.getLaboratorios()));
    }

    @Transactional(readOnly = true)
    public boolean vinculadoLaboratorio(Usuario usuario, Laboratorio laboratorio, LocalDate hoje) {
        if (usuario.atuaNoLaboratorio(laboratorio)) {
            return true; // one of the person's laboratories, assigned by Gestão
        }
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), unidade())
            .stream().anyMatch(v -> v.vigente(hoje) && v.getLaboratorio().getId().equals(laboratorio.getId()));
    }

    @Transactional(readOnly = true)
    public boolean vinculadoProjeto(Usuario usuario, Projeto projeto, LocalDate hoje) {
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), unidade())
            .stream().anyMatch(v -> v.vigente(hoje) && v.pertenceAoProjeto(projeto));
    }

    // Granting labs and contexts is what authorizes operations, so only gestão may change it.
    private static void exigirGestao() {
        if (!PerfilPolicy.isGestao(CurrentUser.get().perfil())) {
            throw new AccessDeniedException("Apenas gestão pode alterar vínculos e laboratórios.");
        }
    }

    /**
     * Vínculos e laboratórios definem onde a pessoa pode operar, então mexer neles é
     * mexer no acesso dela. Por isso valem as mesmas regras da troca de perfil em
     * UsuarioService: ninguém altera o próprio acesso, e só se altera quem tem um perfil
     * que você poderia conceder (ex.: um GESTOR não mexe nos vínculos de um ADMINISTRADOR
     * nem de outro GESTOR).
     */
    private static void exigirAutoridadeSobre(Usuario alvo) {
        AuthenticatedUser ator = CurrentUser.get();
        if (ator.publicId().equals(alvo.getPublicId())) {
            throw new AccessDeniedException("Ninguém pode alterar os próprios vínculos ou laboratórios.");
        }
        if (!PerfilPolicy.podeConceder(ator.perfil(), alvo.getPerfil())) {
            throw new AccessDeniedException("Seu perfil não pode alterar os vínculos de um usuário " + alvo.getPerfil() + ".");
        }
    }
}
