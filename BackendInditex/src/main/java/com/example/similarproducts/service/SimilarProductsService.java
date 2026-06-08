package com.example.similarproducts.service;

import com.example.similarproducts.client.CatalogClient;
import com.example.similarproducts.dto.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    /**
     * Upper bound on detail look-ups fetched concurrently for a single request. Keeps the
     * fan-out bounded (a bulkhead against an unexpectedly long similar-ids list) while still
     * allowing the few items we typically see to be fetched in parallel rather than one by one.
     */
    private static final int DETAIL_FETCH_CONCURRENCY = 8;

    private final CatalogClient catalogClient;

    public SimilarProductsService(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    /**
     * Resolves similar product ids and fans out to fetch each product's detail concurrently,
     * preserving the similarity order from the upstream response. A product whose detail
     * cannot be retrieved (not found, upstream error or timeout) is silently dropped instead
     * of failing the whole request — the contract explicitly allows a shorter-than-requested
     * list (minItems: 0).
     */
    public Mono<List<ProductDetail>> getSimilarProducts(String productId) {
        return catalogClient.getSimilarProductIds(productId)
                .flatMapMany(Flux::fromIterable)
                .flatMapSequential(this::fetchDetailTolerantly, DETAIL_FETCH_CONCURRENCY)
                .collectList();
    }

    private Mono<ProductDetail> fetchDetailTolerantly(String similarProductId) {
        return catalogClient.getProductDetail(similarProductId)
                .onErrorResume(error -> {
                    log.warn("Dropping similar product {} from response: {}", similarProductId, error.toString());
                    return Mono.empty();
                });
    }
}
