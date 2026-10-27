package com.sgl.security;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // sgl.jwt.secret has no default outside dev/demo/test, so startup fails naming SGL_JWT_SECRET.
    @Bean
    public JwtService jwtService(@Value("${sgl.jwt.secret}") String secret,
            @Value("${sgl.jwt.expiration:PT5H}") Duration expiration, Clock clock) {
        return new JwtService(secret, expiration, clock);
    }
}
