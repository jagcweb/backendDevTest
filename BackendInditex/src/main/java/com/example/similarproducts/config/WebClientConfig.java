package com.example.similarproducts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Hard ceiling on a single HTTP exchange with the catalog service. Per-call timeouts
     * applied in {@code CatalogClient} are expected to be lower than this; this is just the
     * last line of defence so a misbehaving connection can never hang a request indefinitely.
     */
    private static final Duration CONNECTOR_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public WebClient catalogWebClient(CatalogProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(CONNECTOR_RESPONSE_TIMEOUT);

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
