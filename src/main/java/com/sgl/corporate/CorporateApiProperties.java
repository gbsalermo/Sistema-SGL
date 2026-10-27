package com.sgl.corporate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the corporate API gateway. Credentials belong to the
 * SGL system account and come from the environment, never from source control.
 */
@ConfigurationProperties("sgl.corporate")
public record CorporateApiProperties(String baseUrl, String ambiente, String username, String password) {
}
