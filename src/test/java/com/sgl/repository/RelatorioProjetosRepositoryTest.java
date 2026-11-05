package com.sgl.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Atividade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.model.Sci;
import com.sgl.model.Unidade;
import com.sgl.tenant.TenantProvider;

@DataJpaTest
@ActiveProfiles("test")
@Import(TenantProvider.class)
class RelatorioProjetosRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ProjetoRepository projetos;
    @Autowired SciRepository scis;
    @Autowired AtividadeRepository atividades;

    private Unidade unidade(String sigla) {
        return em.persistAndFlush(Unidade.builder().nome("Unidade " + sigla).sigla(sigla).build());
    }

    private Laboratorio laboratorio(Unidade unidade) {
        return em.persistAndFlush(Laboratorio.builder().nome("Lab " + unidade.getSigla())
                .unidade(unidade).ativo(true).build());
    }

    private Projeto projeto(Laboratorio laboratorio, String nome, LocalDate inicio, LocalDate fim) {
        return em.persistAndFlush(Projeto.builder()
                .unidade(laboratorio.getUnidade())
                .laboratorios(new LinkedHashSet<>(Set.of(laboratorio)))
                .nome(nome)
                .dataInicio(inicio).dataFim(fim).ativo(true).build());
    }

    private Sci sci(Projeto projeto, String codigo, boolean ativo) {
        return em.persistAndFlush(Sci.builder().projeto(projeto).codigoSeg(codigo)
                .nome(codigo).dataInicio(LocalDate.of(2026, 1, 1))
                .ativo(ativo).build());
    }

    private Atividade atividade(Sci sci, String codigo, boolean ativo) {
        return em.persistAndFlush(Atividade.builder().sci(sci).codigoSeg(codigo)
                .nome(codigo).dataInicio(LocalDate.of(2026, 2, 1))
                .ativo(ativo).build());
    }

    @Test
    void filtroDePeriodoSobrepostoEUnidadeNaoVazamProjetos() {
        Unidade a = unidade("RPA");
        Unidade b = unidade("RPB");
        Laboratorio la = laboratorio(a);
        Laboratorio lb = laboratorio(b);
        Projeto esperado = projeto(la, "A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        projeto(lb, "B", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        projeto(la, "Fora", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        em.clear();

        var encontrados = projetos.buscarParaRelatorio(a.getPublicId(), null, null, null,
                null, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
        assertEquals(1, encontrados.size());
        assertEquals(esperado.getPublicId(), encontrados.get(0).getPublicId());
        assertTrue(projetos.buscarParaRelatorio(b.getPublicId(), la.getPublicId(), null, null,
                null, null, null, null).isEmpty());
    }

    private Projeto projetoComLaboratorios(Unidade unidade, String nome, Laboratorio... labs) {
        return em.persistAndFlush(Projeto.builder()
                .unidade(unidade)
                .laboratorios(new LinkedHashSet<>(List.of(labs)))
                .nome(nome)
                .dataInicio(LocalDate.of(2026, 1, 1)).ativo(true).build());
    }

    @Test
    void projetoComDoisLaboratoriosApareceUmaVez() {
        Unidade u = unidade("RML");
        Projeto p = projetoComLaboratorios(u, "Multi", laboratorio(u), em.persistAndFlush(
                Laboratorio.builder().nome("Outro lab").unidade(u).ativo(true).build()));
        em.clear();

        var encontrados = projetos.buscarParaRelatorio(u.getPublicId(), null, null, null,
                null, null, null, null);
        assertEquals(1, encontrados.size());
        assertEquals(p.getPublicId(), encontrados.get(0).getPublicId());
    }

    @Test
    void filtroPorCadaLaboratorioEncontraOProjeto() {
        Unidade u = unidade("RMF");
        Laboratorio a = laboratorio(u);
        Laboratorio b = em.persistAndFlush(Laboratorio.builder().nome("Lab B").unidade(u).ativo(true).build());
        projetoComLaboratorios(u, "Multi", a, b);
        em.clear();

        assertEquals(1, projetos.buscarParaRelatorio(u.getPublicId(), a.getPublicId(), null, null,
                null, null, null, null).size());
        assertEquals(1, projetos.buscarParaRelatorio(u.getPublicId(), b.getPublicId(), null, null,
                null, null, null, null).size());
    }

    @Test
    void agregacoesIncluemHistoricoMasRespeitamUnidadeEProjetosSelecionados() {
        Unidade a = unidade("RCA");
        Unidade b = unidade("RCB");
        Projeto p1 = projeto(laboratorio(a), "A", LocalDate.of(2026, 1, 1), null);
        Projeto p2 = projeto(laboratorio(a), "Sem descendentes", LocalDate.of(2026, 1, 1), null);
        Projeto outro = projeto(laboratorio(b), "Outra unidade", LocalDate.of(2026, 1, 1), null);

        Sci s1 = sci(p1, "SCI-RCA-1", true);
        Sci s2 = sci(p1, "SCI-RCA-2", false);
        Sci so = sci(outro, "SCI-RCB-1", true);
        atividade(s1, "ATV-RCA-1", true);
        atividade(s1, "ATV-RCA-2", false);
        atividade(s2, "ATV-RCA-3", true);
        atividade(so, "ATV-RCB-1", true);
        em.clear();

        List<UUID> ids = List.of(p1.getPublicId(), p2.getPublicId(), outro.getPublicId());
        var totaisSci = scis.contarScisPorProjetos(a.getPublicId(), ids);
        var totaisAtividade = atividades.contarAtividadesPorProjetos(a.getPublicId(), ids);

        assertEquals(1, totaisSci.size());
        assertEquals(p1.getPublicId(), totaisSci.get(0).getProjetoId());
        assertEquals(2L, totaisSci.get(0).getTotalScis());
        assertEquals(1, totaisAtividade.size());
        assertEquals(p1.getPublicId(), totaisAtividade.get(0).getProjetoId());
        assertEquals(3L, totaisAtividade.get(0).getTotalAtividades());
    }
}
