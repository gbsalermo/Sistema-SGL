package com.sgl.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;

/**
 * Contexto derivado pela hierarquia, somente leitura. "aberta" nao equivale a
 * "elegivel": a elegibilidade considera vigencia, estado e Unidade.
 */
public record ContextoOperacionalResponseDTO(
        String origem,
        UUID participacaoId,
        UUID vinculoEstagioId,
        UUID usuarioId,
        String usuarioNome,
        UUID atividadeId,
        String atividadeNome,
        String atividadeCodigoSeg,
        UUID sciId,
        String sciNome,
        UUID projetoId,
        String projetoNome,
        List<LaboratorioResumoDTO> laboratorios,
        UUID unidadeId,
        LocalDate dataInicioParticipacao,
        LocalDate dataFimParticipacao,
        boolean aberta,
        boolean elegivel,
        String motivoInelegibilidade) {

    public static ContextoOperacionalResponseDTO deParticipacao(
            VinculoEstagioAtividade p, boolean elegivel, String motivoInelegibilidade) {

        VinculoEstagio vinculo = p.getVinculoEstagio();
        Estagiario usuario = vinculo == null ? null : vinculo.getEstagiario();
        Atividade atividade = p.getAtividade();
        Sci sci = atividade == null ? null : atividade.getSci();
        Projeto projeto = sci == null ? null : sci.getProjeto();

        return new ContextoOperacionalResponseDTO(
                "PARTICIPACAO_ESTAGIO",
                p.getPublicId(),
                vinculo == null ? null : vinculo.getPublicId(),
                usuario == null ? null : usuario.getPublicId(),
                usuario == null ? null : usuario.getNome(),
                atividade == null ? null : atividade.getPublicId(),
                atividade == null ? null : atividade.getNome(),
                atividade == null ? null : atividade.getCodigoSeg(),
                sci == null ? null : sci.getPublicId(),
                sci == null ? null : sci.getNome(),
                projeto == null ? null : projeto.getPublicId(),
                projeto == null ? null : projeto.getNome(),
                // All of the project's laboratories feed the estagiário's laboratory selector.
                projeto == null ? List.of() : LaboratorioResumoDTO.de(projeto),
                projeto == null || projeto.getUnidade() == null
                        ? null : projeto.getUnidade().getPublicId(),
                p.getDataInicioParticipacao(),
                p.getDataFimParticipacao(),
                p.getDataFimParticipacao() == null,
                elegivel,
                motivoInelegibilidade);
    }
}
