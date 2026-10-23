package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.sgl.dto.request.VinculoOperacionalUsuarioRequestDTO;
import com.sgl.dto.request.EditarVinculoOperacionalRequestDTO;
import com.sgl.dto.response.VinculoOperacionalUsuarioResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.*;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.*;
import com.sgl.tenant.TenantContext;

/** Contrato de domínio homologado em DEV; autorização de gestor depende do futuro SSO. */
@Service @RequiredArgsConstructor
public class VinculoOperacionalUsuarioService {
    private final VinculoOperacionalUsuarioRepository vinculos;
    private final UsuarioRepository usuarios;
    private final LaboratorioRepository laboratorios;
    private final ProjetoRepository projetos;
    private final SciRepository scis;
    private final AtividadeRepository atividades;

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
        UUID tenant = unidade();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(dto.getUsuarioId(), tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", dto.getUsuarioId()));
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
            atividade = atividades.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(dto.getAtividadeId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", dto.getAtividadeId()));
            atividade.validateActive();
            sci = atividade.getSci();
            projeto = sci.getProjeto();
        } else if (dto.getSciId() != null) {
            sci = scis.findByPublicIdAndProjetoLaboratorioUnidadePublicId(dto.getSciId(), tenant)
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
            if (!projeto.getLaboratorio().getId().equals(laboratorio.getId())) {
                throw new BusinessRuleException("Projeto/SCI/Atividade não pertence ao laboratório informado.");
            }
        }
        if (dto.getProjetoId() != null && (projeto == null || !dto.getProjetoId().equals(projeto.getPublicId()))
            || dto.getSciId() != null && (sci == null || !dto.getSciId().equals(sci.getPublicId()))) {
            throw new BusinessRuleException("Contexto hierárquico divergente.");
        }
        // O laboratório-base já é um vínculo operacional completo com esse laboratório.
        // Permitir somente associações mais específicas (Projeto, SCI ou Atividade).
        if (projeto == null && sci == null && atividade == null
                && usuario.getLaboratorio() != null
                && usuario.getLaboratorio().getId().equals(laboratorio.getId())) {
            throw new BusinessRuleException("Este já é o laboratório-base do usuário. Para esse laboratório, associe apenas Projeto, SCI ou Atividade.");
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
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        if (Boolean.TRUE.equals(v.getEncerrado())) {
            throw new BusinessRuleException("Vínculo encerrado não pode ser editado.");
        }
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(v.getDataInicio())) {
            throw new BusinessRuleException("A data final não pode ser anterior à data de início.");
        }
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Para encerrar vínculo imediatamente, utilize Encerrar vínculo.");
        }
        v.setDataFim(dto.getDataFim());
        v.setObservacao(dto.getObservacao());
        return new VinculoOperacionalUsuarioResponseDTO(v);
    }

    @Transactional
    public VinculoOperacionalUsuarioResponseDTO encerrar(UUID id) {
        VinculoOperacionalUsuario v = vinculos.findByPublicIdAndUsuarioUnidadePublicId(id, unidade())
            .orElseThrow(() -> new ResourceNotFoundException("Vínculo operacional", id));
        if (Boolean.TRUE.equals(v.getEncerrado())) {
            throw new BusinessRuleException("Vínculo já encerrado.");
        }
        v.setDataFim(LocalDate.now());
        v.setEncerrado(true);
        return new VinculoOperacionalUsuarioResponseDTO(v);
    }

    @Transactional
    public void definirLaboratorioBase(UUID usuarioId, UUID laboratorioId) {
        UUID tenant = unidade();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(usuarioId, tenant)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário", usuarioId));
        if (usuario.getPerfil() == Perfil.ESTAGIARIO) {
            throw new BusinessRuleException("Laboratório do estagiário é gerenciado pelo fluxo de estágio.");
        }
        Laboratorio lab = laboratorioId == null ? null :
            laboratorios.findByPublicIdAndUnidadePublicId(laboratorioId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratório", laboratorioId));
        if (lab != null) {
            lab.validateActive();
            // Evita introduzir duplicidade ao promover um laboratório adicional a base.
            boolean possuiVinculoGenerico = vinculos
                .findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), tenant)
                .stream().anyMatch(v -> v.vigente(LocalDate.now())
                    && v.getLaboratorio().getId().equals(lab.getId())
                    && v.getProjeto() == null && v.getSci() == null && v.getAtividade() == null);
            if (possuiVinculoGenerico && (usuario.getLaboratorio() == null
                    || !usuario.getLaboratorio().getId().equals(lab.getId()))) {
                throw new BusinessRuleException("Este laboratório já possui vínculo adicional vigente. Encerre o vínculo genérico antes de defini-lo como laboratório-base.");
            }
        }
        usuario.setLaboratorio(lab);
    }

    @Transactional(readOnly = true)
    public boolean vinculadoLaboratorio(Usuario usuario, Laboratorio laboratorio, LocalDate hoje) {
        if (usuario.getLaboratorio() != null && usuario.getLaboratorio().getId().equals(laboratorio.getId())) {
            return true; // laboratório-base, atribuído pela Gestão em ambiente controlado
        }
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), unidade())
            .stream().anyMatch(v -> v.vigente(hoje) && v.getLaboratorio().getId().equals(laboratorio.getId()));
    }

    @Transactional(readOnly = true)
    public boolean vinculadoProjeto(Usuario usuario, Projeto projeto, LocalDate hoje) {
        return vinculos.findByUsuarioPublicIdAndUsuarioUnidadePublicId(usuario.getPublicId(), unidade())
            .stream().anyMatch(v -> v.vigente(hoje) && v.pertenceAoProjeto(projeto));
    }
}
