package com.sgl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.model.enums.SituacaoEstagio;
import com.sgl.repository.EstagiarioRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.VinculoEstagioRepository;

/**
 * Valida a massa ficticia do perfil DEMO usando H2 do perfil test.
 * A massa e instanciada manualmente, sem ativar o perfil demo em producao.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class DemoDataInitializerIntegrationTest {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private EstagiarioRepository estagiarioRepository;

    @Autowired
    private VinculoEstagioRepository vinculoEstagioRepository;

    @Autowired
    private VinculoEstagioAtividadeRepository participacaoRepository;

    @Test
    @Transactional
    void semeiaSeisEstagiariosAtivosComVinculoEParticipacaoEHistoricoEncerrado() throws Exception {
        beanFactory.createBean(DemoDataInitializer.class).run();

        var unidade = unidadeRepository.findAllSemFiltroDeTenant().stream()
                .filter(u -> "CNPMF".equals(u.getSigla()))
                .findFirst().orElseThrow();

        var estagiarios = estagiarioRepository.findByUnidadePublicId(unidade.getPublicId());
        assertEquals(8, estagiarios.size(), "DEMO deve conter seis estagiarios ativos e dois encerrados");
        assertEquals(6, estagiarios.stream().filter(e -> Boolean.TRUE.equals(e.getAtivo())).count());
        assertEquals(2, estagiarios.stream().filter(e -> !Boolean.TRUE.equals(e.getAtivo())).count());

        var vinculos = vinculoEstagioRepository.findAll();
        assertEquals(6, vinculos.size(), "Cada estagiario ativo deve ter um vinculo institucional");
        assertTrue(vinculos.stream().allMatch(v -> v.getSituacao() == SituacaoEstagio.EM_ANDAMENTO
                && v.getDataFimEfetiva() == null
                && v.getDataFimPrevista().isAfter(LocalDate.now())
                && v.getOrientador() != null));

        var participacoes = participacaoRepository.findAll();
        assertEquals(6, participacoes.size(), "Cada vinculo ativo deve participar de uma atividade");
        assertTrue(participacoes.stream().allMatch(p ->
                p.getDataFimParticipacao() == null && p.getAtividade() != null));

        assertEquals(6, estagiarioRepository
                .findEstagiariosComVinculoAtivo(unidade.getPublicId()).size(),
                "A listagem operacional deve localizar os seis estagiarios ativos");
    }
}
