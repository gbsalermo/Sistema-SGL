package com.sgl.corporate;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Replaces the corporate gateway with a {@link MockRestServiceServer} in a full Spring context.
 *
 * CorporateApiConfig sets the JDK request factory on the builder right before building the client,
 * which would overwrite a mock factory bound to a shared builder bean. So instead of fighting that
 * ordering, this registers a second, @Primary client built from a builder bound to the mock server.
 * Every test class that imports this config (with the same other annotations) shares one context.
 */
@TestConfiguration
public class CorporateGatewayTestConfig {

    private final RestClient.Builder builder = RestClient.builder();

    @Bean
    MockRestServiceServer corporateGatewayServer() {
        return MockRestServiceServer.bindTo(builder).build();
    }

    // Takes the server as a parameter so the mock is bound before the client builds its RestClient.
    @Bean
    @Primary
    CorporateApiClient mockedCorporateApiClient(CorporateApiProperties properties, MockRestServiceServer server) {
        return new CorporateApiClient(builder, properties);
    }

    @Bean
    CorporateGatewayStubs corporateGatewayStubs(CorporateApiProperties properties, MockRestServiceServer server) {
        return new CorporateGatewayStubs(server, properties.baseUrl(), properties.ambiente());
    }
}
