package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

@DataJpaTest
@ActiveProfiles("test")
class AtividadeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AtividadeRepository atividadeRepository;

    private Unidade criarUnidade(String sigla) {
        return entityManager.persistAndFlush(
                Unidade.builder()
                        .nome("Unidade " + sigla)
                        .sigla(sigla)
                        .build()
        );
    }

    private Laboratorio criarLaboratorio(Unidade unidade, String nome) {
        return entityManager.persistAndFlush(
                Laboratorio.builder()
                        .unidade(unidade)
                        .nome(nome)
                        .ativo(true)
                        .build()
        );
    }

    private Projeto criarProjeto(
            Laboratorio laboratorio,
            String nome,
            LocalDate inicio) {

        return entityManager.persistAndFlush(
                Projeto.builder()
                        .laboratorio(laboratorio)
                        .nome(nome)
                        .dataInicio(inicio)
                        .ativo(true)
                        .build()
        );
    }

    private Sci criarSci(
            Projeto projeto,
            String codigoSeg,
            String nome) {

        return entityManager.persistAndFlush(
                Sci.builder()
                        .projeto(projeto)
                        .codigoSeg(codigoSeg)
                        .nome(nome)
                        .dataInicio(projeto.getDataInicio())
                        .status(StatusProjeto.ATIVO)
                        .situacaoExecucao(SituacaoExecucaoProjeto.NAO_INFORMADO)
                        .ativo(true)
                        .build()
        );
    }

    private Atividade criarAtividade(
            Sci sci,
            String codigoSeg,
            String nome,
            boolean ativo) {

        return entityManager.persistAndFlush(
                Atividade.builder()
                        .sci(sci)
                        .codigoSeg(codigoSeg)
                        .nome(nome)
                        .responsavel("Responsável")
                        .dataInicio(sci.getDataInicio())
                        .status(StatusProjeto.ATIVO)
                        .situacaoExecucao(SituacaoExecucaoProjeto.NAO_INFORMADO)
                        .ativo(ativo)
                        .build()
        );
    }

    @Test
    void devePersistirCamposDaAtividade() {
        Unidade unidade = criarUnidade("AR1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório Atividade");
        Projeto projeto = criarProjeto(
                laboratorio,
                "Projeto Atividade",
                LocalDate.of(2026, 1, 1)
        );
        Sci sci = criarSci(
                projeto,
                "93.93.93.001.01.01",
                "SCI Atividade"
        );

        Atividade atividade = Atividade.builder()
                .sci(sci)
                .codigoSeg("93.93.93.001.01.01.001")
                .nome("Atividade Persistida")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 5, 31))
                .status(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO)
                .ativo(true)
                .build();

        entityManager.persistAndFlush(atividade);
        entityManager.clear();

        Optional<Atividade> resultado =
                atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        atividade.getPublicId(),
                        unidade.getPublicId()
                );

        assertTrue(resultado.isPresent());
        assertEquals("93.93.93.001.01.01.001", resultado.get().getCodigoSeg());
        assertEquals(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE, resultado.get().getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO, resultado.get().getSituacaoExecucao());
        assertEquals(sci.getPublicId(), resultado.get().getSci().getPublicId());
        assertEquals(projeto.getPublicId(), resultado.get().getSci().getProjeto().getPublicId());
    }

    @Test
    void deveIsolarBuscaPorPublicIdEntreUnidades() {
        Unidade unidadeA = criarUnidade("AR2");
        Unidade unidadeB = criarUnidade("AR3");

        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório A");
        Projeto projetoA = criarProjeto(
                laboratorioA,
                "Projeto A",
                LocalDate.of(2026, 1, 1)
        );
        Sci sciA = criarSci(
                projetoA,
                "93.93.93.002.01.01",
                "SCI A"
        );
        Atividade atividadeA = criarAtividade(
                sciA,
                "93.93.93.002.01.01.001",
                "Atividade A",
                true
        );

        Optional<Atividade> resultado =
                atividadeRepository.findByPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        atividadeA.getPublicId(),
                        unidadeB.getPublicId()
                );

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarAtividadesSomenteDoSciInformadoEDoTenant() {
        Unidade unidade = criarUnidade("AR4");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório B");
        Projeto projeto = criarProjeto(
                laboratorio,
                "Projeto B",
                LocalDate.of(2026, 1, 1)
        );

        Sci sciA = criarSci(
                projeto,
                "93.93.93.003.01.01",
                "SCI B1"
        );
        Sci sciB = criarSci(
                projeto,
                "93.93.93.003.01.02",
                "SCI B2"
        );

        criarAtividade(
                sciA,
                "93.93.93.003.01.01.001",
                "Atividade B1",
                true
        );
        criarAtividade(
                sciB,
                "93.93.93.003.01.02.001",
                "Atividade B2",
                true
        );

        List<Atividade> resultado =
                atividadeRepository.findBySciPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        sciA.getPublicId(),
                        unidade.getPublicId()
                );

        assertEquals(1, resultado.size());
        assertEquals("Atividade B1", resultado.get(0).getNome());
    }

    @Test
    void deveListarAtividadesPorProjeto() {
        Unidade unidade = criarUnidade("AR5");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório C");

        Projeto projetoA = criarProjeto(
                laboratorio,
                "Projeto C1",
                LocalDate.of(2026, 1, 1)
        );
        Projeto projetoB = criarProjeto(
                laboratorio,
                "Projeto C2",
                LocalDate.of(2026, 1, 1)
        );

        Sci sciA = criarSci(projetoA, "93.93.93.004.01.01", "SCI C1");
        Sci sciB = criarSci(projetoB, "93.93.93.005.01.01", "SCI C2");

        criarAtividade(sciA, "93.93.93.004.01.01.001", "Atividade C1", true);
        criarAtividade(sciB, "93.93.93.005.01.01.001", "Atividade C2", true);

        List<Atividade> resultado =
                atividadeRepository.findBySciProjetoPublicIdAndSciProjetoLaboratorioUnidadePublicId(
                        projetoA.getPublicId(),
                        unidade.getPublicId()
                );

        assertEquals(1, resultado.size());
        assertEquals("Atividade C1", resultado.get(0).getNome());
    }

    @Test
    void deveListarSomenteAtividadesAtivasDaUnidade() {
        Unidade unidade = criarUnidade("AR6");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório D");
        Projeto projeto = criarProjeto(
                laboratorio,
                "Projeto D",
                LocalDate.of(2026, 1, 1)
        );
        Sci sci = criarSci(
                projeto,
                "93.93.93.006.01.01",
                "SCI D"
        );

        criarAtividade(
                sci,
                "93.93.93.006.01.01.001",
                "Atividade Ativa",
                true
        );
        criarAtividade(
                sci,
                "93.93.93.006.01.01.002",
                "Atividade Inativa",
                false
        );

        List<Atividade> resultado =
                atividadeRepository.findBySciProjetoLaboratorioUnidadePublicIdAndAtivoTrue(
                        unidade.getPublicId()
                );

        assertEquals(1, resultado.size());
        assertEquals("Atividade Ativa", resultado.get(0).getNome());
    }
}
