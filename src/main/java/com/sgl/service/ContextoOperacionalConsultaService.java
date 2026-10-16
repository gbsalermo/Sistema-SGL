package com.sgl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.dto.response.ContextoOperacionalResponseDTO;
import com.sgl.exception.BusinessRuleException;
import com.sgl.exception.ResourceNotFoundException;
import com.sgl.model.Usuario;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.repository.AtividadeRepository;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UsuarioRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * Consultas read-only para 7.5.2.
 *
 * O isolamento de dados e aplicado nas queries por Unidade do usuario E
 * Unidade da Atividade. Autorizacao por identidade (titular/gestor) exige
 * principal autenticado e integra a etapa definitiva de seguranca.
 */
@Service
@RequiredArgsConstructor
public class ContextoOperacionalConsultaService {

    private final VinculoEstagioAtividadeRepository participacoes;
    private final UsuarioRepository usuarios;
    private final AtividadeRepository atividades;
    private final ProjetoRepository projetos;

    @Transactional(readOnly = true)
    public List<ContextoOperacionalResponseDTO> listarDoEstagiario(UUID usuarioId) {
        UUID unidadeId = exigirTenant();
        Usuario usuario = usuarios.findByPublicIdAndUnidadePublicId(usuarioId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        if (usuario.getPerfil() != Perfil.ESTAGIARIO) {
            throw new BusinessRuleException(
                    "A consulta de participacoes e exclusiva de usuarios com perfil ESTAGIARIO.");
        }

        LocalDate hoje = LocalDate.now();
        return participacoes.listarContextosDoUsuario(usuarioId, unidadeId)
                .stream().map(p -> resumir(p, hoje, unidadeId)).toList();
    }

    @Transactional(readOnly = true)
    public List<ContextoOperacionalResponseDTO> listarDaAtividade(UUID atividadeId) {
        UUID unidadeId = exigirTenant();
        atividades.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(atividadeId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Atividade", atividadeId));

        LocalDate hoje = LocalDate.now();
        return participacoes.listarParticipacoesDaAtividade(atividadeId, unidadeId)
                .stream().map(p -> resumir(p, hoje, unidadeId)).toList();
    }

    @Transactional(readOnly = true)
    public List<ContextoOperacionalResponseDTO> listarDoProjeto(UUID projetoId) {
        UUID unidadeId = exigirTenant();
        projetos.findByPublicIdAndLaboratorioUnidadePublicId(projetoId, unidadeId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", projetoId));

        LocalDate hoje = LocalDate.now();
        return participacoes.listarParticipacoesDoProjeto(projetoId, unidadeId)
                .stream().map(p -> resumir(p, hoje, unidadeId)).toList();
    }

    private ContextoOperacionalResponseDTO resumir(
            VinculoEstagioAtividade p, LocalDate hoje, UUID unidadeId) {
        var resultado = ElegibilidadeContextoOperacional.avaliar(p, hoje, unidadeId);
        return ContextoOperacionalResponseDTO.deParticipacao(
                p, resultado.elegivel(),
                resultado.elegivel() ? null : resultado.motivo().name());
    }

    private UUID exigirTenant() {
        return TenantContext.unidadeAtual().orElseThrow(
                () -> new BusinessRuleException(
                        "Cabecalho X-SGL-Unidade-Id e obrigatorio para esta operacao."));
    }
}
