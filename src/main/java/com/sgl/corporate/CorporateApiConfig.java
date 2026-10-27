package com.sgl.corporate;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CorporateApiProperties.class)
class CorporateApiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    // The app has no auto-configured RestClient.Builder bean, hence the fallback.
    // The request factory is set here (not in the client) so tests can bind a
    // MockRestServiceServer to the builder without it being overridden.
    @Bean
    CorporateApiClient corporateApiClient(ObjectProvider<RestClient.Builder> builderProvider, CorporateApiProperties properties) {
        validate(properties);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return new CorporateApiClient(builderProvider.getIfAvailable(RestClient::builder).requestFactory(factory), properties);
    }

    // Fails fast with an actionable message instead of an obscure error from
    // setBasicAuth when the environment variables are missing.
    static void validate(CorporateApiProperties p) {
        require(p.baseUrl(), "sgl.corporate.base-url", "SGL_CORPORATE_BASE_URL");
        require(p.ambiente(), "sgl.corporate.ambiente", "SGL_CORPORATE_AMBIENTE");
        require(p.username(), "sgl.corporate.username", "SGL_CORPORATE_USERNAME");
        require(p.password(), "sgl.corporate.password", "SGL_CORPORATE_PASSWORD");
    }

    private static void require(String value, String property, String envVar) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Corporate API configuration missing: set " + envVar + " (property " + property + ")");
        }
    }
}
