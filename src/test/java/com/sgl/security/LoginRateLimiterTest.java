package com.sgl.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-27T12:00:00Z"));
    private final LoginRateLimiter limiter = new LoginRateLimiter(clock);

    @Test
    void cincoFalhas_bloqueiaASexta() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("m1|1.1.1.1");
        }
        assertThat(limiter.isBlocked("m1|1.1.1.1")).isFalse();

        limiter.recordFailure("m1|1.1.1.1");

        assertThat(limiter.isBlocked("m1|1.1.1.1")).isTrue();
    }

    @Test
    void chavesSaoIndependentes() {
        falhar("m1|1.1.1.1", 5);

        assertThat(limiter.isBlocked("m1|2.2.2.2")).isFalse();
        assertThat(limiter.isBlocked("m2|1.1.1.1")).isFalse();
    }

    @Test
    void falhasAntigas_saemDaJanelaDeQuinzeMinutos() {
        falhar("m1|1.1.1.1", 5);

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThat(limiter.isBlocked("m1|1.1.1.1")).isFalse();
    }

    @Test
    void sucesso_zeraContador() {
        falhar("m1|1.1.1.1", 4);

        limiter.reset("m1|1.1.1.1");
        limiter.recordFailure("m1|1.1.1.1");

        assertThat(limiter.isBlocked("m1|1.1.1.1")).isFalse();
    }

    private void falhar(String key, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limiter.recordFailure(key);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
