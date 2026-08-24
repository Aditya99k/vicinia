package com.vicinia.catalogservice.dto;

import com.vicinia.catalogservice.domain.Category;

public record CategoryResponse(String id, String name, String slug) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug());
    }
}
