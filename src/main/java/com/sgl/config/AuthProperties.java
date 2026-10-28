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
     * Single-use bootstrap for a fresh deployment: while no ADMINISTRADOR_GERAL has ever been
     * registered (active or not), the next new corporate user becomes one. Off by default
     * (SGL_PRIMEIRO_LOGIN_ADMIN). Once any ADMINISTRADOR_GERAL exists it has no effect, even if
     * that administrator is later deactivated, so leaving it on afterwards grants nothing.
     */
    private boolean primeiroLoginAdmin = false;
}
