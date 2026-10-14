package com.sgl.service;

import static com.sgl.service.ElegibilidadeContextoOperacional.Motivo.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sgl.model.Atividade;
import com.sgl.model.Estagiario;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.VinculoEstagio;
import com.sgl.model.VinculoEstagioAtividade;
import com.sgl.model.enums.Perfil;
import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;
import com.sgl.service.ElegibilidadeContextoOperacional.Motivo;

/** Teste de politica pura: nao conecta banco nem depende de session/Clock global. */
class ElegibilidadeContextoOperacionalTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);
    private static final UUID UNIDADE = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OUTRA_UNIDADE = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private VinculoEstagioAtividade participacao;
    private VinculoEstagio vinculo;
    private Estagiario estagiario;
    private Atividade atividade;
    private Sci sci;
    private Projeto projeto;
    private Laboratorio laboratorio;

    @BeforeEach
    void preparar() {
        Unidade unidade = Unidade.builder().publicId(UNIDADE).nome("Unidade de testes").build();
        laboratorio = Laboratorio.builder()
                .unidade(unidade).nome("Laboratorio A").ativo(true).build();

        projeto = Projeto.builder()
                .laboratorio(laboratorio).nome("Projeto A")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true).build();
        sci = Sci.builder()
                .projeto(projeto).nome("SCI A")
                .dataInicio(LocalDate.of(2026, 1, 1))
                .dataFim(LocalDate.of(2026, 12, 31))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true).build();
        atividade = Atividade.builder()
                .sci(sci).nome("Atividade A")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 11, 30))
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_NO_PRAZO)
                .ativo(true).build();

        estagiario = new Estagiario();
        estagiario.setUnidade(unidade);
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setAtivo(true);
        // O laboratorio legado do usuario nao integra a politica de elegibilidade.
        estagiario.setLaboratorio(null);

        vinculo = new VinculoEstagio();
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 11, 30));
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);

        participacao = new VinculoEstagioAtividade();
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));
    }

    private void bloqueado(Motivo motivo) {
        var resultado = ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, UNIDADE);
        assertFalse(resultado.elegivel());
        assertEquals(motivo, resultado.motivo());
    }

    @Test
    void permiteParticipacaoVigenteSemLaboratorioLegadoDoEstagiario() {
        var resultado = ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, UNIDADE);
        assertTrue(resultado.elegivel());
        assertEquals(ELEGIVEL, resultado.motivo());
    }

    @Test
    void permiteSituacaoInstitucionalProrrogada() {
        vinculo.setSituacao(SituacaoEstagio.PRORROGADO);
        assertTrue(ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, UNIDADE).elegivel());
    }

    @Test
    void permiteDuasParticipacoesEmLaboratoriosDiferentesDaMesmaUnidade() {
        Unidade unidade = laboratorio.getUnidade();
        Laboratorio segundoLaboratorio = Laboratorio.builder()
                .unidade(unidade).ativo(true).nome("Laboratorio B").build();
        Projeto segundoProjeto = Projeto.builder()
                .laboratorio(segundoLaboratorio).ativo(true)
                .status(StatusProjeto.ATIVO)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO)
                .dataInicio(LocalDate.of(2026, 1, 1)).build();
        Sci segundoSci = Sci.builder()
                .projeto(segundoProjeto).ativo(true)
                .status(StatusProjeto.ATIVO)
                .dataInicio(LocalDate.of(2026, 1, 1)).build();
        Atividade segundaAtividade = Atividade.builder()
                .sci(segundoSci).ativo(true)
                .status(StatusProjeto.ATIVO)
                .dataInicio(LocalDate.of(2026, 3, 1)).build();

        VinculoEstagioAtividade outraParticipacao = new VinculoEstagioAtividade();
        outraParticipacao.setVinculoEstagio(vinculo);
        outraParticipacao.setAtividade(segundaAtividade);
        outraParticipacao.setDataInicioParticipacao(LocalDate.of(2026, 4, 1));

        assertTrue(ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, UNIDADE).elegivel());
        assertTrue(ElegibilidadeContextoOperacional.avaliar(outraParticipacao, HOJE, UNIDADE).elegivel());
    }

    @Test
    void bloqueiaUsuarioInativo() {
        estagiario.setAtivo(false);
        bloqueado(USUARIO_INATIVO);
    }

    @Test
    void bloqueiaUsuarioComPerfilDiferenteDeEstagiario() {
        estagiario.setPerfil(Perfil.PESQUISADOR);
        bloqueado(PERFIL_INCOMPATIVEL);
    }

    @Test
    void bloqueiaVinculoFinalizadoOuComFimEfetivo() {
        vinculo.setSituacao(SituacaoEstagio.FINALIZADO);
        bloqueado(VINCULO_FINALIZADO);
        vinculo.setSituacao(SituacaoEstagio.EM_ANDAMENTO);
        vinculo.setDataFimEfetiva(LocalDate.of(2026, 10, 10));
        bloqueado(VINCULO_FINALIZADO);
    }

    @Test
    void bloqueiaVinculoFuturoOuComPrazoVencido() {
        vinculo.setDataInicio(LocalDate.of(2026, 10, 15));
        bloqueado(VINCULO_FORA_DA_VIGENCIA);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 10, 13));
        bloqueado(VINCULO_FORA_DA_VIGENCIA);
    }

    @Test
    void bloqueiaParticipacaoEncerradaMesmoComVinculoEmAndamento() {
        participacao.setDataFimParticipacao(LocalDate.of(2026, 10, 10));
        bloqueado(PARTICIPACAO_ENCERRADA);
    }

    @Test
    void bloqueiaParticipacaoComInicioFuturo() {
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 10, 15));
        bloqueado(PARTICIPACAO_FORA_DA_VIGENCIA);
    }

    @Test
    void bloqueiaCruzamentoDeTenantOuUnidade() {
        var resultado = ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, OUTRA_UNIDADE);
        assertFalse(resultado.elegivel());
        assertEquals(UNIDADE_DIVERGENTE, resultado.motivo());
    }

    @Test
    void bloqueiaQuandoProjetoPertenceAOutraUnidade() {
        Unidade outra = Unidade.builder().publicId(OUTRA_UNIDADE).build();
        laboratorio.setUnidade(outra);
        bloqueado(UNIDADE_DIVERGENTE);
    }

    @Test
    void bloqueiaQualquerNivelInativo() {
        atividade.setAtivo(false);
        bloqueado(HIERARQUIA_INATIVA);
        atividade.setAtivo(true);
        sci.setAtivo(false);
        bloqueado(HIERARQUIA_INATIVA);
        sci.setAtivo(true);
        projeto.setAtivo(false);
        bloqueado(HIERARQUIA_INATIVA);
        projeto.setAtivo(true);
        laboratorio.setAtivo(false);
        bloqueado(HIERARQUIA_INATIVA);
    }

    @Test
    void bloqueiaEstadoEncerradoOuExecucaoCancelada() {
        atividade.setStatus(StatusProjeto.CONCLUIDO);
        bloqueado(HIERARQUIA_NAO_OPERACIONAL);
        atividade.setStatus(StatusProjeto.ATIVO);
        sci.setSituacaoExecucao(SituacaoExecucaoProjeto.EXECUCAO_CANCELADA);
        bloqueado(HIERARQUIA_NAO_OPERACIONAL);
    }

    @Test
    void bloqueiaHierarquiaForaDoPeriodo() {
        atividade.setDataFim(LocalDate.of(2026, 10, 13));
        bloqueado(HIERARQUIA_FORA_DA_VIGENCIA);
        atividade.setDataFim(null);
        sci.setDataInicio(LocalDate.of(2026, 10, 15));
        bloqueado(HIERARQUIA_FORA_DA_VIGENCIA);
    }

    @Test
    void permiteDatasLimiteInclusivas() {
        atividade.setDataFim(HOJE);
        vinculo.setDataFimPrevista(HOJE);
        sci.setDataFim(HOJE);
        projeto.setDataFim(HOJE);
        assertTrue(ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, UNIDADE).elegivel());
    }

    @Test
    void falhaFechadoQuandoReferenciaOuDataObrigatoriaAusente() {
        participacao.setDataInicioParticipacao(null);
        bloqueado(CONTEXTO_INCOMPLETO);
        participacao.setDataInicioParticipacao(LocalDate.of(2026, 3, 1));
        assertEquals(CONTEXTO_INCOMPLETO,
                ElegibilidadeContextoOperacional.avaliar(participacao, HOJE, null).motivo());
    }
}
