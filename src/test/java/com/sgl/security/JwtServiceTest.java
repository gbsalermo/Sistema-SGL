package com.sgl.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-long!!";
    private static final Duration EXPIRATION = Duration.ofHours(5);
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private final JwtService service = new JwtService(SECRET, EXPIRATION, clockAt(NOW));

    @Test
    void generate_eParse_devolvemMesmoPublicId() {
        UUID publicId = UUID.randomUUID();

        String token = service.generate(publicId);

        assertThat(service.parse(token)).contains(publicId);
        assertThat(service.expiresAt(token)).isEqualTo(NOW.plus(EXPIRATION));
    }

    @Test
    void parse_tokenExpirado_vazio() {
        String token = service.generate(UUID.randomUUID());
        JwtService later = new JwtService(SECRET, EXPIRATION, clockAt(NOW.plus(Duration.ofHours(5).plusMinutes(1))));

        assertThat(later.parse(token)).isEmpty();
    }

    @Test
    void parse_assinadoComOutroSegredo_vazio() {
        JwtService other = new JwtService("another-secret-with-32-bytes-or-more!!", EXPIRATION, clockAt(NOW));

        assertThat(service.parse(other.generate(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void parse_lixo_vazio() {
        assertThat(service.parse("abc")).isEmpty();
    }

    @Test
    void construtor_segredoCurto_lancaIllegalArgument() {
        assertThatThrownBy(() -> new JwtService("short", EXPIRATION, clockAt(NOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SGL_JWT_SECRET");
    }
}
