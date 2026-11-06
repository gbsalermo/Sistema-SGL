package com.sgl.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.sgl.model.Unidade;
import com.sgl.repository.UnidadeRepository;
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
