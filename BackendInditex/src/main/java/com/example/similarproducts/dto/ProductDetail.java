package com.example.similarproducts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Mirrors the {@code ProductDetail} schema agreed in similarProducts.yaml. Unknown fields
 * from the upstream catalog response are ignored so this contract stays stable even if the
 * upstream adds fields we don't care about.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDetail(
        String id,
        String name,
        BigDecimal price,
        boolean availability) {
}
