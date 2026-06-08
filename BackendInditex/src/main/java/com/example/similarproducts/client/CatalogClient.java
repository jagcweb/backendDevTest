package com.example.similarproducts.client;

import com.example.similarproducts.config.CatalogProperties;
import com.example.similarproducts.dto.ProductDetail;
import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.exception.UpstreamServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Thin reactive wrapper around the existing catalog endpoints documented in existingApis.yaml
 * (similar ids + product detail, both served from {@code catalog.base-url}).
 */
@Component
public class CatalogClient {

    private final WebClient webClient;
    private final CatalogProperties properties;

    public CatalogClient(WebClient catalogWebClient, CatalogProperties properties) {
        this.webClient = catalogWebClient;
        this.properties = properties;
    }

    /**
     * Resolves the ordered list of similar product ids for {@code productId}.
     * A 404 here means the product itself is unknown, so it is translated into
     * {@link ProductNotFoundException} and propagated to the caller as-is. Any other
     * failure (5xx, timeout, connection error) becomes an {@link UpstreamServiceException}
     * since we cannot build a meaningful response without this list.
     */
    public Mono<List<String>> getSimilarProductIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.statusCode().value() == 404
                                ? Mono.error(new ProductNotFoundException(productId))
                                : Mono.error(new UpstreamServiceException(
                                        "Catalog returned " + response.statusCode() + " for similar ids of product " + productId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new UpstreamServiceException(
                                "Catalog returned " + response.statusCode() + " for similar ids of product " + productId)))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() { })
                .timeout(properties.similarIdsTimeout())
                .onErrorMap(TimeoutException.class, e ->
                        new UpstreamServiceException("Timed out resolving similar ids for product " + productId, e));
    }

    /**
     * Fetches the detail of a single product. Deliberately left to surface raw errors
     * (404 / 5xx / timeout) — the caller decides whether a missing detail should drop the
     * product from the result rather than fail the whole aggregation.
     */
    public Mono<ProductDetail> getProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .bodyToMono(ProductDetail.class)
                .timeout(properties.productDetailTimeout());
    }
}
