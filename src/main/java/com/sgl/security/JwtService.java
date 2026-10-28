package com.sgl.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues and validates HS256 session tokens. The token carries only the usuario publicId (subject):
 * perfil, unidade and CPF are resolved from the database on each request so that changes apply
 * immediately and no personal data leaks through the token.
 */
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(String secret, Duration expiration, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "SGL_JWT_SECRET (sgl.jwt.secret) must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.clock = clock;
    }

    public String generate(UUID usuarioPublicId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(usuarioPublicId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Returns empty for expired, tampered or malformed tokens, never throws. */
    public Optional<UUID> parse(String token) {
        return claims(token).flatMap(c -> {
            try {
                return Optional.of(UUID.fromString(c.getSubject()));
            } catch (IllegalArgumentException | NullPointerException e) {
                return Optional.empty();
            }
        });
    }

    public Instant expiresAt(String token) {
        return claims(token).map(c -> c.getExpiration().toInstant())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
    }

    private Optional<Claims> claims(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
