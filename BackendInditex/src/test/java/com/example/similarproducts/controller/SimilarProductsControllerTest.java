package com.example.similarproducts.controller;

import com.example.similarproducts.dto.ProductDetail;
import com.example.similarproducts.exception.ProductNotFoundException;
import com.example.similarproducts.exception.UpstreamServiceException;
import com.example.similarproducts.service.SimilarProductsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private SimilarProductsService similarProductsService;

    @Test
    void returnsSimilarProductDetailsAsJsonArray() {
        when(similarProductsService.getSimilarProducts("1")).thenReturn(Mono.just(List.of(
                new ProductDetail("2", "Dress", BigDecimal.valueOf(19.99), true),
                new ProductDetail("4", "Boots", BigDecimal.valueOf(39.99), true)
        )));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .hasSize(2);
    }

    @Test
    void returns404WhenProductDoesNotExistUpstream() {
        when(similarProductsService.getSimilarProducts("404"))
                .thenReturn(Mono.error(new ProductNotFoundException("404")));

        webTestClient.get().uri("/product/404/similar")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returns503WhenUpstreamCannotResolveSimilarIds() {
        when(similarProductsService.getSimilarProducts("1"))
                .thenReturn(Mono.error(new UpstreamServiceException("upstream unavailable")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
