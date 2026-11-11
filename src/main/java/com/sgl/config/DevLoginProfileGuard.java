package com.sgl.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Aborts startup when the developer-only login is combined with the prod profile.
 *
 * <p>Profiles are additive, so a stray {@code prod} flag next to {@code dev} or {@code demo}
 * would otherwise expose test users in a production environment. Failing fast at boot is
 * safer than relying on each dev bean to remember to check.
 */
@Component
@Profile({"dev", "demo"})
class DevLoginProfileGuard {

    private static final Profiles PROD = Profiles.of("prod");

    private final Environment environment;

    DevLoginProfileGuard(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void verificar() {
        if (environment.acceptsProfiles(PROD)) {
            throw new IllegalStateException(
                    "Login de teste (dev/demo) não pode ser ativado junto com o profile prod.");
        }
    }
}
