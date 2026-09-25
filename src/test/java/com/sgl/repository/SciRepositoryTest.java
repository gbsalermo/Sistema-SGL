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

import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.model.enums.SituacaoExecucaoProjeto;
import com.sgl.model.enums.StatusProjeto;

@DataJpaTest
@ActiveProfiles("test")
class SciRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SciRepository sciRepository;

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

    private Projeto criarProjeto(Laboratorio laboratorio, String nome, LocalDate inicio, LocalDate fim) {
        return entityManager.persistAndFlush(
                Projeto.builder()
                        .laboratorio(laboratorio)
                        .nome(nome)
                        .dataInicio(inicio)
                        .dataFim(fim)
                        .ativo(true)
                        .build()
        );
    }

    private Sci criarSci(Projeto projeto, String codigoSeg, String nome, boolean ativo) {
        return entityManager.persistAndFlush(
                Sci.builder()
                        .projeto(projeto)
                        .codigoSeg(codigoSeg)
                        .nome(nome)
                        .responsavel("Responsável")
                        .dataInicio(projeto.getDataInicio())
                        .status(StatusProjeto.ATIVO)
                        .situacaoExecucao(SituacaoExecucaoProjeto.NAO_INFORMADO)
                        .ativo(ativo)
                        .build()
        );
    }

    @Test
    void devePersistirCamposDaSci() {
        Unidade unidade = criarUnidade("SR1");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório SCI");
        Projeto projeto = criarProjeto(
                laboratorio,
                "Projeto SCI",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );

        Sci sci = Sci.builder()
                .projeto(projeto)
                .codigoSeg("94.94.94.001.01.01")
                .nome("SCI Persistido")
                .responsavel("Pesquisador")
                .dataInicio(LocalDate.of(2026, 2, 1))
                .dataFim(LocalDate.of(2026, 5, 31))
                .status(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE)
                .situacaoExecucao(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO)
                .ativo(true)
                .build();

        entityManager.persistAndFlush(sci);
        entityManager.clear();

        Optional<Sci> resultado =
                sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
                        sci.getPublicId(),
                        unidade.getPublicId()
                );

        assertTrue(resultado.isPresent());
        assertEquals("94.94.94.001.01.01", resultado.get().getCodigoSeg());
        assertEquals(StatusProjeto.ENCERRADO_COM_AVALIACAO_PENDENTE, resultado.get().getStatus());
        assertEquals(SituacaoExecucaoProjeto.EM_ANDAMENTO_ATRASADO, resultado.get().getSituacaoExecucao());
        assertEquals(projeto.getPublicId(), resultado.get().getProjeto().getPublicId());
    }

    @Test
    void deveIsolarBuscaPorPublicIdEntreUnidades() {
        Unidade unidadeA = criarUnidade("SR2");
        Unidade unidadeB = criarUnidade("SR3");
        Laboratorio laboratorioA = criarLaboratorio(unidadeA, "Laboratório A");
        Projeto projetoA = criarProjeto(laboratorioA, "Projeto A", LocalDate.of(2026, 1, 1), null);
        Sci sciA = criarSci(projetoA, "94.94.94.002.01.01", "SCI A", true);

        Optional<Sci> resultado =
                sciRepository.findByPublicIdAndProjetoLaboratorioUnidadePublicId(
                        sciA.getPublicId(),
                        unidadeB.getPublicId()
                );

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarSciSomenteDoProjetoInformadoEDoTenant() {
        Unidade unidade = criarUnidade("SR4");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório B");
        Projeto projetoA = criarProjeto(laboratorio, "Projeto B1", LocalDate.of(2026, 1, 1), null);
        Projeto projetoB = criarProjeto(laboratorio, "Projeto B2", LocalDate.of(2026, 1, 1), null);

        criarSci(projetoA, "94.94.94.003.01.01", "SCI B1", true);
        criarSci(projetoB, "94.94.94.004.01.01", "SCI B2", true);

        List<Sci> resultado =
                sciRepository.findByProjetoPublicIdAndProjetoLaboratorioUnidadePublicId(
                        projetoA.getPublicId(),
                        unidade.getPublicId()
                );

        assertEquals(1, resultado.size());
        assertEquals("SCI B1", resultado.get(0).getNome());
    }

    @Test
    void deveListarSomenteSciAtivosDaUnidade() {
        Unidade unidade = criarUnidade("SR5");
        Laboratorio laboratorio = criarLaboratorio(unidade, "Laboratório C");
        Projeto projeto = criarProjeto(laboratorio, "Projeto C", LocalDate.of(2026, 1, 1), null);

        criarSci(projeto, "94.94.94.005.01.01", "SCI Ativo", true);
        criarSci(projeto, "94.94.94.005.01.02", "SCI Inativo", false);

        List<Sci> resultado =
                sciRepository.findByProjetoLaboratorioUnidadePublicIdAndAtivoTrue(
                        unidade.getPublicId()
                );

        assertEquals(1, resultado.size());
        assertEquals("SCI Ativo", resultado.get(0).getNome());
    }
}
