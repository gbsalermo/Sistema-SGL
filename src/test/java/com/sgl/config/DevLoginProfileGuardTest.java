package com.sgl.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class DevLoginProfileGuardTest {

    @Test
    void aceitaDev() {
        DevLoginProfileGuard guard = new DevLoginProfileGuard(new MockEnvironment().withProperty("spring.profiles.active", "dev"));

        assertThatCode(guard::verificar).doesNotThrowAnyException();
    }

    @Test
    void aceitaDemo() {
        DevLoginProfileGuard guard = new DevLoginProfileGuard(new MockEnvironment().withProperty("spring.profiles.active", "demo"));

        assertThatCode(guard::verificar).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "demo"})
    void recusaPerfilDeTesteJuntoComProd(String perfilDeTeste) {
        // Dev login mints sessions without a corporate identity, so a single stray prod flag must abort startup.
        DevLoginProfileGuard guard = new DevLoginProfileGuard(
                new MockEnvironment().withProperty("spring.profiles.active", perfilDeTeste + ",prod"));

        assertThatThrownBy(guard::verificar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }
}
