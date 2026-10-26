package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

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
import com.sgl.model.enums.TipoBolsa;
import com.sgl.tenant.TenantProvider;

@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class VinculoEstagioAtividadeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Autowired
    private VinculoEstagioAtividadeRepository participacaoRepository;

    private Unidade criarUnidade(String sigla) {
        return entityManager.persistAndFlush(
                Unidade.builder()
                        .nome("Unidade " + sigla)
                        .sigla(sigla)
                        .build());
    }

    private Laboratorio criarLaboratorio(Unidade unidade) {
        return entityManager.persistAndFlush(
                Laboratorio.builder()
                        .unidade(unidade)
                        .nome("Laboratório " + unidade.getSigla())
                        .ativo(true)
                        .build());
    }

    private Estagiario criarEstagiario(
            Unidade unidade,
            Laboratorio laboratorio,
            String email) {

        Estagiario estagiario = new Estagiario();
        estagiario.setNome("Estagiário " + email);
        estagiario.setEmail(email);
        estagiario.setSenha("senha");
        estagiario.setPerfil(Perfil.ESTAGIARIO);
        estagiario.setUnidade(unidade);
        estagiario.setLaboratorio(laboratorio);
        estagiario.setAtivo(true);
        estagiario.setDataInicioEstagio(LocalDate.of(2026, 1, 1));
        estagiario.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        estagiario.setSituacaoEstagio(SituacaoEstagio.EM_ANDAMENTO);

        return entityManager.persistAndFlush(estagiario);
    }

    private VinculoEstagio criarVinculo(
            Estagiario estagiario,
            SituacaoEstagio situacao) {

        VinculoEstagio vinculo = new VinculoEstagio();
        vinculo.setEstagiario(estagiario);
        vinculo.setDataInicio(LocalDate.of(2026, 1, 1));
        vinculo.setDataFimPrevista(LocalDate.of(2026, 12, 31));
        vinculo.setTipoBolsa(TipoBolsa.BOLSA_CNPQ);
        vinculo.setSituacao(situacao);

        if (situacao == SituacaoEstagio.FINALIZADO) {
            vinculo.setDataFimEfetiva(LocalDate.of(2026, 12, 31));
        }

        return entityManager.persistAndFlush(vinculo);
    }

    private Atividade criarAtividade(
            Laboratorio laboratorio,
            String sufixo) {

        Projeto projeto = Projeto.builder()
                .laboratorio(laboratorio)
                .nome("Projeto " + sufixo)
                .codigoSeg("PRJ-" + sufixo)
                .ativo(true)
                .build();
        projeto = entityManager.persistAndFlush(projeto);

        Sci sci = Sci.builder()
                .projeto(projeto)
                .codigoSeg("SCI-" + sufixo)
                .nome("SCI " + sufixo)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .build();
        sci = entityManager.persistAndFlush(sci);

        Atividade atividade = Atividade.builder()
                .sci(sci)
                .codigoSeg("ATV-" + sufixo)
                .nome("Atividade " + sufixo)
                .dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(true)
                .build();

        return entityManager.persistAndFlush(atividade);
    }

    private VinculoEstagioAtividade criarParticipacao(
            VinculoEstagio vinculo,
            Atividade atividade,
            LocalDate inicio,
            LocalDate fim) {

        VinculoEstagioAtividade participacao =
                new VinculoEstagioAtividade();
        participacao.setVinculoEstagio(vinculo);
        participacao.setAtividade(atividade);
        participacao.setDataInicioParticipacao(inicio);
        participacao.setDataFimParticipacao(fim);

        return entityManager.persistAndFlush(participacao);
    }

    @Test
    void deveBuscarParticipacaoPorPublicIdETenant() {
        Unidade unidade = criarUnidade("VA1");
        Laboratorio laboratorio = criarLaboratorio(unidade);
        Estagiario estagiario =
                criarEstagiario(unidade, laboratorio, "va1@teste.com");
        VinculoEstagio vinculo =
                criarVinculo(estagiario, SituacaoEstagio.EM_ANDAMENTO);
        Atividade atividade = criarAtividade(laboratorio, "VA1");

        VinculoEstagioAtividade participacao =
                criarParticipacao(
                        vinculo,
                        atividade,
                        LocalDate.of(2026, 2, 1),
                        null);

        Optional<VinculoEstagioAtividade> resultado =
                participacaoRepository
                        .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                                participacao.getPublicId(),
                                unidade.getPublicId());

        assertTrue(resultado.isPresent());
        assertEquals(participacao.getId(), resultado.get().getId());
    }

    @Test
    void naoDeveBuscarParticipacaoDeOutraUnidade() {
        Unidade unidade = criarUnidade("VA2");
        Unidade outraUnidade = criarUnidade("VA3");
        Laboratorio laboratorio = criarLaboratorio(unidade);
        Estagiario estagiario =
                criarEstagiario(unidade, laboratorio, "va2@teste.com");
        VinculoEstagio vinculo =
                criarVinculo(estagiario, SituacaoEstagio.EM_ANDAMENTO);
        Atividade atividade = criarAtividade(laboratorio, "VA2");

        VinculoEstagioAtividade participacao =
                criarParticipacao(
                        vinculo,
                        atividade,
                        LocalDate.of(2026, 2, 1),
                        null);

        Optional<VinculoEstagioAtividade> resultado =
                participacaoRepository
                        .findByPublicIdAndVinculoEstagioEstagiarioUnidadePublicId(
                                participacao.getPublicId(),
                                outraUnidade.getPublicId());

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarHistoricoEmOrdemDecrescenteEFiltrarAbertas() {
        Unidade unidade = criarUnidade("VA4");
        Laboratorio laboratorio = criarLaboratorio(unidade);
        Estagiario estagiario =
                criarEstagiario(unidade, laboratorio, "va4@teste.com");
        VinculoEstagio vinculo =
                criarVinculo(estagiario, SituacaoEstagio.EM_ANDAMENTO);
        Atividade atividade = criarAtividade(laboratorio, "VA4");

        VinculoEstagioAtividade antiga =
                criarParticipacao(
                        vinculo,
                        atividade,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 2, 28));

        VinculoEstagioAtividade atual =
                criarParticipacao(
                        vinculo,
                        atividade,
                        LocalDate.of(2026, 3, 1),
                        null);

        List<VinculoEstagioAtividade> historico =
                participacaoRepository
                        .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdOrderByDataInicioParticipacaoDesc(
                                vinculo.getPublicId(),
                                unidade.getPublicId());

        List<VinculoEstagioAtividade> abertas =
                participacaoRepository
                        .findByVinculoEstagioPublicIdAndVinculoEstagioEstagiarioUnidadePublicIdAndDataFimParticipacaoIsNull(
                                vinculo.getPublicId(),
                                unidade.getPublicId());

        assertEquals(2, historico.size());
        assertEquals(atual.getId(), historico.get(0).getId());
        assertEquals(antiga.getId(), historico.get(1).getId());

        assertEquals(1, abertas.size());
        assertEquals(atual.getId(), abertas.get(0).getId());

        assertTrue(participacaoRepository
                .existsByVinculoEstagioIdAndAtividadeIdAndDataFimParticipacaoIsNull(
                        vinculo.getId(),
                        atividade.getId()));

        assertEquals(1L,
                participacaoRepository
                        .countByVinculoEstagioIdAndDataFimParticipacaoIsNull(
                                vinculo.getId()));
    }

    @Test
    void deveDetectarVinculoNaoFinalizadoDoEstagiario() {
        Unidade unidade = criarUnidade("VA5");
        Laboratorio laboratorio = criarLaboratorio(unidade);
        Estagiario estagiario =
                criarEstagiario(unidade, laboratorio, "va5@teste.com");

        criarVinculo(estagiario, SituacaoEstagio.EM_ANDAMENTO);

        assertTrue(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO));
    }

    @Test
    void naoDeveDetectarVinculoNaoFinalizadoQuandoSoHaHistoricoFinalizado() {
        Unidade unidade = criarUnidade("VA6");
        Laboratorio laboratorio = criarLaboratorio(unidade);
        Estagiario estagiario =
                criarEstagiario(unidade, laboratorio, "va6@teste.com");

        criarVinculo(estagiario, SituacaoEstagio.FINALIZADO);

        assertFalse(vinculoEstagioRepository
                .existsByEstagiarioIdAndSituacaoNot(
                        estagiario.getId(),
                        SituacaoEstagio.FINALIZADO));
    }

    @Test
    void relatorioContaParticipacoesHistoricasEDeduplicaEstagiarioEntreProjetos() {
        Unidade unidadeA = criarUnidade("VR1");
        Unidade unidadeB = criarUnidade("VR2");
        Laboratorio labA = criarLaboratorio(unidadeA);
        Laboratorio labB = criarLaboratorio(unidadeB);

        Estagiario pessoa = criarEstagiario(unidadeA, labA, "vr1@teste.com");
        VinculoEstagio vinculo = criarVinculo(pessoa, SituacaoEstagio.EM_ANDAMENTO);

        Atividade atividade1 = criarAtividade(labA, "VR1A");
        Atividade atividade2 = criarAtividade(labA, "VR1B");
        criarParticipacao(vinculo, atividade1, LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 1));
        criarParticipacao(vinculo, atividade1, LocalDate.of(2026, 3, 2), null);
        criarParticipacao(vinculo, atividade2, LocalDate.of(2026, 4, 1), null);

        Estagiario outraPessoa = criarEstagiario(unidadeB, labB, "vr2@teste.com");
        VinculoEstagio outroVinculo = criarVinculo(outraPessoa, SituacaoEstagio.EM_ANDAMENTO);
        Atividade atividadeOutraUnidade = criarAtividade(labB, "VR2A");
        criarParticipacao(outroVinculo, atividadeOutraUnidade, LocalDate.of(2026, 2, 1), null);

        UUID projeto1 = atividade1.getSci().getProjeto().getPublicId();
        UUID projeto2 = atividade2.getSci().getProjeto().getPublicId();
        List<UUID> ids = List.of(projeto1, projeto2,
                atividadeOutraUnidade.getSci().getProjeto().getPublicId());

        entityManager.flush();
        entityManager.clear();

        var agregacoes = participacaoRepository.contarParticipacoesPorProjetos(
                unidadeA.getPublicId(), ids);

        assertEquals(2, agregacoes.size());
        var a = agregacoes.stream()
                .filter(item -> projeto1.equals(item.getProjetoId()))
                .findFirst().orElseThrow();
        var b = agregacoes.stream()
                .filter(item -> projeto2.equals(item.getProjetoId()))
                .findFirst().orElseThrow();

        assertEquals(2L, a.getTotalParticipacoes());
        assertEquals(1L, a.getTotalEstagiarios());
        assertEquals(1L, b.getTotalParticipacoes());
        assertEquals(1L, b.getTotalEstagiarios());
        assertEquals(1L, participacaoRepository.contarEstagiariosDistintosNosProjetos(
                unidadeA.getPublicId(), ids));
    }
}
