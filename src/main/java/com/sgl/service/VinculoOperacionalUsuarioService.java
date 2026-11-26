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
import com.sgl.security.PapelPolicy;
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
        exigirAdministrador();
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
        // Everyone reads their own links (the pedido, solução and resíduo screens need them).
        if (!CurrentUser.get().publicId().equals(usuarioId)) {
            exigirAdministrador();
        }
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuarioId, unidade())
            .stream().map(VinculoOperacionalUsuarioResponseDTO::new).toList();
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO criar(VinculoOperacionalUsuarioRequestDTO dto) {
        exigirAdministrador();
        UUID tenant = unidade();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(dto.getUsuarioId(), tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));
        exigirAutoridadeSobre(usuario);
        return criarVinculo(dto, usuario);
    }

    /**
     * Creates a link to the laboratory whose team is being managed; the caller has already checked who
     * manages that team, so only "nobody grants access to themselves" is checked here.
     */
    @Transactional
    public VinculoOperacionalUsuarioResponseDTO criarNaEquipe(Laboratorio laboratorio, VinculoOperacionalUsuarioRequestDTO dto) {
        if (!laboratorio.getPublicId().equals(dto.getLaboratorioId())) {
            throw new BusinessRuleException("O acesso temporário precisa ser para o laboratório desta equipe.");
        }
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(dto.getUsuarioId(), unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));
        exigirOutraPessoa(usuario);
        return criarVinculo(dto, usuario);
    }

    @Transactional(readOnly = true)
    public List<VinculoOperacionalUsuarioResponseDTO> listarDaEquipe(Laboratorio laboratorio) {
        LocalDate hoje = LocalDate.now();
        return vinculos.findByUsuarioUnidadePublicId(unidade()).stream()
            .filter(v -> v.getLaboratorio().getId().equals(laboratorio.getId()))
            .sorted(java.util.Comparator.comparing((VinculoOperacionalUsuario v) -> !v.vigente(hoje))
                .thenComparing(VinculoOperacionalUsuario::getDataInicio, java.util.Comparator.reverseOrder()))
            .map(VinculoOperacionalUsuarioResponseDTO::new)
            .toList();
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO editarNaEquipe(Laboratorio laboratorio, UUID id, EditarVinculoOperacionalRequestDTO dto) {
        VinculoOperacionalUsuario v = vinculoDaEquipe(laboratorio, id);
        exigirOutraPessoa(v.getUsuario());
        return editarVinculo(v, dto);
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO encerrarNaEquipe(Laboratorio laboratorio, UUID id) {
        VinculoOperacionalUsuario v = vinculoDaEquipe(laboratorio, id);
        exigirOutraPessoa(v.getUsuario());
        return encerrarVinculo(v);
    }

    // A link of another laboratory is "not found" through this team, as if it did not exist.
    private VinculoOperacionalUsuario vinculoDaEquipe(Laboratorio laboratorio, UUID id) {
        return vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .filter(v -> v.getLaboratorio().getId().equals(laboratorio.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
    }

    private static void exigirOutraPessoa(Usuario alvo) {
        if (CurrentUser.get().publicId().equals(alvo.getPublicId())) {
            throw new AccessDeniedException("Ninguém concede acesso a si mesmo.");
        }
    }

    private VinculoOperacionalUsuarioResponseDTO criarVinculo(VinculoOperacionalUsuarioRequestDTO dto, Usuario usuario) {
        UUID tenant = unidade();
        if (usuario.isEstagiario()) {
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
        exigirAdministrador();
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        exigirAutoridadeSobre(v.getUsuario());
        return editarVinculo(v, dto);
    }

    private VinculoOperacionalUsuarioResponseDTO editarVinculo(VinculoOperacionalUsuario v, EditarVinculoOperacionalRequestDTO dto) {
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
        exigirAdministrador();
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        exigirAutoridadeSobre(v.getUsuario());
        return encerrarVinculo(v);
    }

    private static VinculoOperacionalUsuarioResponseDTO encerrarVinculo(VinculoOperacionalUsuario v) {
        if (Boolean.TRUE.equals(v.getEncerrado())) {
            throw new BusinessRuleException("Vínculo já encerrado.");
        }
        v.setDataFim(LocalDate.now());
        v.setEncerrado(true);
        return new VinculoOperacionalUsuarioResponseDTO(v);
    }

    @Transactional
    public void definirLaboratorios(UUID usuarioId, List<UUID> laboratorioIds) {
        exigirAdministrador();
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
            laboratorioIds, usuario.getUnidade(), usuario.getPapeis(), usuario.getLaboratorios()));
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
    // Team management left central gestão: each laboratory's responsável does it through its team;
    // centrally, only administrators keep these routes.
    private static void exigirAdministrador() {
        if (!PapelPolicy.isAdministrador(CurrentUser.get().papeis())) {
            throw new AccessDeniedException("Apenas administradores alteram vínculos e laboratórios fora da equipe do laboratório.");
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
        if (!PapelPolicy.podeAlterar(ator.papeis(), alvo.getPapeis())) {
            throw new AccessDeniedException("Seus papéis não permitem alterar os vínculos de um usuário com papéis " + alvo.getPapeis() + ".");
        }
    }
}
