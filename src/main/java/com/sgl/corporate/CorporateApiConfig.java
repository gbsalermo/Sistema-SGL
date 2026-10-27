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
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(READ_TIMEOUT);
        return new CorporateApiClient(builderProvider.getIfAvailable(RestClient::builder).requestFactory(factory), properties);
    }
}
