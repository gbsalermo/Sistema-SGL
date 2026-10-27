package com.sgl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/** Session/login switches under {@code sgl.auth}. */
@Component
@ConfigurationProperties("sgl.auth")
@Getter
@Setter
public class AuthProperties {

    /**
     * Bootstrap for a fresh deployment: while no active ADMINISTRADOR_GERAL exists, the next new
     * corporate user becomes one. Off by default (SGL_PRIMEIRO_LOGIN_ADMIN) and inert once an
     * ADMINISTRADOR_GERAL exists, so leaving it on afterwards grants nothing.
     */
    private boolean primeiroLoginAdmin = false;
}
