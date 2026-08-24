package com.vicinia.catalogservice.dto;

import com.vicinia.catalogservice.domain.Product;

import java.util.List;
import java.util.Map;

public record ProductResponse(
        String id,
        String name,
        String brand,
        String category,
        String description,
        List<String> images,
        Map<String, Object> attributes,
        String status,
        String requestedByUserId,
        String rejectionReason
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getDescription(),
                p.getImages(), p.getAttributes(), p.getStatus().name(),
                p.getRequestedByUserId(), p.getRejectionReason()
        );
    }
}
