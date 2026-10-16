package com.sgl.service;

import java.time.LocalDate;
import java.util.UUID;

import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

/**
 * Politica de elegibilidade de uma participacao para originar uma NOVA
 * solicitacao. Nao altera o status institucional nem o historico.
 *
 * Preparada na 7.5.1 para as consultas read-only da 7.5.2; Pedidos passam
 * a consumir esse contrato somente na Etapa 8.
 */
public final class ElegibilidadeContextoOperacional {

    private ElegibilidadeContextoOperacional() {
    }

    public enum Motivo {
        ELEGIVEL,
        CONTEXTO_INCOMPLETO,
        PERFIL_INCOMPATIVEL,
        USUARIO_INATIVO,
        VINCULO_FINALIZADO,
        VINCULO_FORA_DA_VIGENCIA,
        PARTICIPACAO_ENCERRADA,
        PARTICIPACAO_FORA_DA_VIGENCIA,
        UNIDADE_DIVERGENTE,
        HIERARQUIA_INATIVA,
        HIERARQUIA_NAO_OPERACIONAL,
        HIERARQUIA_FORA_DA_VIGENCIA
    }

    public record Resultado(boolean elegivel, Motivo motivo) {
        public static Resultado permitido() {
            return new Resultado(true, Motivo.ELEGIVEL);
        }

        public static Resultado bloqueado(Motivo motivo) {
            return new Resultado(false, motivo);
        }
    }

    /**
     * @param participacao Participacao que identifica a Atividade de origem.
     * @param dataReferencia Data da solicitacao, fornecida pelo servidor.
     * @param unidadeSolicitante Unidade validada para o solicitante/tenant.
     */
    public static Resultado avaliar(VinculoEstagioAtividade participacao,
            LocalDate dataReferencia, UUID unidadeSolicitante) {

        if (participacao == null || dataReferencia == null || unidadeSolicitante == null) {
            return Resultado.bloqueado(Motivo.CONTEXTO_INCOMPLETO);
        }

        VinculoEstagio vinculo = participacao.getVinculoEstagio();
        Atividade atividade = participacao.getAtividade();
        if (vinculo == null || vinculo.getEstagiario() == null || atividade == null) {
            return Resultado.bloqueado(Motivo.CONTEXTO_INCOMPLETO);
        }

        Estagiario estagiario = vinculo.getEstagiario();
        if (vinculo.getDataInicio() == null || participacao.getDataInicioParticipacao() == null
                || atividade.getDataInicio() == null) {
            return Resultado.bloqueado(Motivo.CONTEXTO_INCOMPLETO);
        }
        if (estagiario.getPerfil() != Perfil.ESTAGIARIO) {
            return Resultado.bloqueado(Motivo.PERFIL_INCOMPATIVEL);
        }
        if (!Boolean.TRUE.equals(estagiario.getAtivo())) {
            return Resultado.bloqueado(Motivo.USUARIO_INATIVO);
        }
        if (vinculo.getSituacao() == null
                || vinculo.getSituacao() == SituacaoEstagio.FINALIZADO
                || vinculo.getDataFimEfetiva() != null) {
            return Resultado.bloqueado(Motivo.VINCULO_FINALIZADO);
        }
        if (!dentroDaVigencia(dataReferencia, vinculo.getDataInicio(), vinculo.getDataFimPrevista())) {
            return Resultado.bloqueado(Motivo.VINCULO_FORA_DA_VIGENCIA);
        }
        if (participacao.getDataFimParticipacao() != null) {
            return Resultado.bloqueado(Motivo.PARTICIPACAO_ENCERRADA);
        }
        if (!dentroDaVigencia(dataReferencia, participacao.getDataInicioParticipacao(), null)) {
            return Resultado.bloqueado(Motivo.PARTICIPACAO_FORA_DA_VIGENCIA);
        }

        Sci sci = atividade.getSci();
        Projeto projeto = sci == null ? null : sci.getProjeto();
        Laboratorio laboratorio = projeto == null ? null : projeto.getLaboratorio();
        if (sci == null || projeto == null || laboratorio == null || laboratorio.getUnidade() == null
                || estagiario.getUnidade() == null) {
            return Resultado.bloqueado(Motivo.CONTEXTO_INCOMPLETO);
        }

        if (sci.getDataInicio() == null) {
            return Resultado.bloqueado(Motivo.CONTEXTO_INCOMPLETO);
        }

        UUID unidadeOrigem = laboratorio.getUnidade().getPublicId();
        UUID unidadeEstagiario = estagiario.getUnidade().getPublicId();
        if (unidadeOrigem == null || unidadeEstagiario == null
                || !unidadeOrigem.equals(unidadeSolicitante)
                || !unidadeOrigem.equals(unidadeEstagiario)) {
            return Resultado.bloqueado(Motivo.UNIDADE_DIVERGENTE);
        }

        if (!Boolean.TRUE.equals(laboratorio.getAtivo())
                || !Boolean.TRUE.equals(projeto.getAtivo())
                || !Boolean.TRUE.equals(sci.getAtivo())
                || !Boolean.TRUE.equals(atividade.getAtivo())) {
            return Resultado.bloqueado(Motivo.HIERARQUIA_INATIVA);
        }

        if (!operacional(projeto.getStatus(), projeto.getSituacaoExecucao())
                || !operacional(sci.getStatus(), sci.getSituacaoExecucao())
                || !operacional(atividade.getStatus(), atividade.getSituacaoExecucao())) {
            return Resultado.bloqueado(Motivo.HIERARQUIA_NAO_OPERACIONAL);
        }

        if (!dentroDaVigencia(dataReferencia, projeto.getDataInicio(), projeto.getDataFim())
                || !dentroDaVigencia(dataReferencia, sci.getDataInicio(), sci.getDataFim())
                || !dentroDaVigencia(dataReferencia, atividade.getDataInicio(), atividade.getDataFim())) {
            return Resultado.bloqueado(Motivo.HIERARQUIA_FORA_DA_VIGENCIA);
        }

        return Resultado.permitido();
    }

    private static boolean operacional(StatusProjeto status, SituacaoExecucaoProjeto execucao) {
        return status == StatusProjeto.ATIVO
                && execucao != SituacaoExecucaoProjeto.EXECUCAO_CANCELADA;
    }

    private static boolean dentroDaVigencia(LocalDate data, LocalDate inicio, LocalDate fim) {
        return (inicio == null || !data.isBefore(inicio))
                && (fim == null || !data.isAfter(fim));
    }
}
