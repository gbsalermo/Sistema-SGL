package com.sgl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.sgl.model.Unidade;
import com.sgl.model.Laboratorio;
import com.sgl.model.Projeto;
import com.sgl.repository.ProjetoRepository;
import com.sgl.repository.UnidadeRepository;
import com.sgl.repository.VinculoEstagioAtividadeRepository;
import com.sgl.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
// The seed writes a large data set into this context's database; do not share it with other tests.
@DirtiesContext
class DataInitializerIntegrationTest {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private VinculoEstagioAtividadeRepository vinculoEstagioAtividadeRepository;

    @Test
    @Transactional
    void semeiaProjetoComDoisLaboratoriosDaMesmaUnidadeComParticipacaoDeEstagiario() throws Exception {
        unidadeRepository.save(new Unidade(null, null, "Embrapa Mandioca e Fruticultura", "CNPMF", null));

        beanFactory.createBean(DataInitializer.class).run();

        Projeto multiLab = projetoRepository.findAll().stream()
                .filter(projeto -> projeto.getLaboratorios().size() == 2)
                .findFirst()
                .orElseThrow(() -> new AssertionError("A massa DEV deve ter um projeto com 2 laboratórios"));
        assertTrue(multiLab.getLaboratorios().stream()
                .map(Laboratorio::getUnidade)
                .allMatch(unidade -> unidade.getId().equals(multiLab.getUnidade().getId())),
                "Os laboratórios do projeto devem pertencer à unidade do projeto");
        long participacoes = vinculoEstagioAtividadeRepository.findAll().stream()
                .filter(p -> p.getAtividade().getSci().getProjeto().getId().equals(multiLab.getId()))
                .count();
        assertTrue(participacoes >= 1, "Deve haver participação de estagiário em atividade do projeto com 2 laboratórios");
        assertEquals(2, multiLab.getLaboratorios().size());
    }

    @Test
    void semeiaMassaDevQuandoSoExisteAUnidadeCriadaPelaMigration() throws Exception {
        // V45 inserts the CNPMF unit in every new database; the test profile has Flyway off,
        // so that state is reproduced by hand.
        unidadeRepository.save(new Unidade(null, null, "Embrapa Mandioca e Fruticultura", "CNPMF", null));

        // DataInitializer is @Profile("dev"); createBean builds it with the real repositories
        // without activating the dev profile (which would point at PostgreSQL).
        DataInitializer initializer = beanFactory.createBean(DataInitializer.class);
        initializer.run();

        assertTrue(usuarioRepository.count() > 0,
                "Com apenas a unidade da migration, a massa DEV (usuários) deve ser criada");
    }
}
