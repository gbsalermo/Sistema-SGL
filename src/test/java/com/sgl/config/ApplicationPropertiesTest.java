package com.sgl.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Guards the base configuration that every deployment inherits, whatever profile it runs. */
class ApplicationPropertiesTest {

    private static final Properties BASE = new Properties();

    @BeforeAll
    static void carregar() throws IOException {
        try (InputStream in = ApplicationPropertiesTest.class.getResourceAsStream("/application.properties")) {
            BASE.load(in);
        }
    }

    @Test
    void naoAtivaPerfilDevPorPadrao() {
        // A deploy that forgets SPRING_PROFILES_ACTIVE must not silently get dev login, dev JWT secret and seed users.
        assertThat(BASE.getProperty("spring.profiles.active")).isNull();
    }

    @Test
    void silenciaLogDoHibernateQueRepeteValoresDeConstraint() {
        // PostgreSQL's "Key (cpf)=(...) already exists" detail would reach the logs unmasked.
        assertThat(BASE.getProperty("logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper")).isEqualTo("OFF");
    }
}
