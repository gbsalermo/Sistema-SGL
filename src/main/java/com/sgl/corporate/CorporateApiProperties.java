package com.sgl.corporate;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection settings for the corporate API gateway. Credentials belong to the
 * SGL system account and come from the environment, never from source control.
 *
 * When {@code required} is false (dev/demo), blank credentials are tolerated and
 * every client call fails as unavailable, so corporate login answers 503.
 */
@ConfigurationProperties("sgl.corporate")
public record CorporateApiProperties(String baseUrl, String ambiente, String username, String password,
                                  @DefaultValue("true") boolean required) {
}
