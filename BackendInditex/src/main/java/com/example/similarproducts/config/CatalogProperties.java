package com.example.similarproducts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Connection settings for the upstream catalog service (similar ids + product detail).
 * Bound from the {@code catalog.*} properties so timeouts can be tuned per environment
 * without touching code.
 */
@ConfigurationProperties(prefix = "catalog")
public record CatalogProperties(
        String baseUrl,
        Duration similarIdsTimeout,
        Duration productDetailTimeout) {
}
