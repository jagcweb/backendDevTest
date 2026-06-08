package com.example.similarproducts.service;

import com.example.similarproducts.client.CatalogClient;
import com.example.similarproducts.dto.ProductDetail;
import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilarProductsServiceTest {

    private final CatalogClient catalogClient = mock(CatalogClient.class);
    private final SimilarProductsService service = new SimilarProductsService(catalogClient);

    @Test
    void preservesSimilarityOrderAndDropsProductsWhoseDetailFails() {
        when(catalogClient.getSimilarProductIds("1")).thenReturn(Mono.just(List.of("2", "3", "4")));
        when(catalogClient.getProductDetail("2")).thenReturn(Mono.just(detail("2", "Dress")));
        when(catalogClient.getProductDetail("3")).thenReturn(Mono.error(new RuntimeException("upstream timed out")));
        when(catalogClient.getProductDetail("4")).thenReturn(Mono.just(detail("4", "Boots")));

        StepVerifier.create(service.getSimilarProducts("1"))
                .assertNext(details -> assertThat(details)
                        .extracting(ProductDetail::id)
                        .containsExactly("2", "4"))
                .verifyComplete();
    }

    @Test
    void returnsEmptyListWhenNoSimilarProductDetailCanBeResolved() {
        when(catalogClient.getSimilarProductIds("1")).thenReturn(Mono.just(List.of("2", "3")));
        when(catalogClient.getProductDetail("2")).thenReturn(Mono.error(new RuntimeException("not found")));
        when(catalogClient.getProductDetail("3")).thenReturn(Mono.error(new RuntimeException("server error")));

        StepVerifier.create(service.getSimilarProducts("1"))
                .assertNext(details -> assertThat(details).isEmpty())
                .verifyComplete();
    }

    @Test
    void propagatesNotFoundWhenUpstreamProductIsUnknown() {
        when(catalogClient.getSimilarProductIds("404")).thenReturn(Mono.error(new ProductNotFoundException("404")));

        StepVerifier.create(service.getSimilarProducts("404"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void propagatesUpstreamErrorWhenSimilarIdsCannotBeResolved() {
        when(catalogClient.getSimilarProductIds("1")).thenReturn(Mono.error(new UpstreamServiceException("boom")));

        StepVerifier.create(service.getSimilarProducts("1"))
                .expectError(UpstreamServiceException.class)
                .verify();
    }

    private static ProductDetail detail(String id, String name) {
        return new ProductDetail(id, name, BigDecimal.valueOf(9.99), true);
    }
}
