package com.vicinia.catalogservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.catalogservice.domain.Product;
import com.vicinia.catalogservice.dto.CategoryResponse;
import com.vicinia.catalogservice.dto.ProductRequest;
import com.vicinia.catalogservice.dto.ProductResponse;
import com.vicinia.catalogservice.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mapped at /api/catalog to match api-gateway's route exactly. Only
 * /products/search and /categories are in the gateway's public-paths —
 * everything else (requesting a new product, viewing your own requests)
 * requires a validated caller.
 */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products/search")
    public List<ProductResponse> search(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String category) {
        return catalogService.search(q, category).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse getById(@PathVariable String id) {
        return ProductResponse.from(catalogService.getById(id));
    }

    @PostMapping("/products/request")
    public ResponseEntity<ProductResponse> requestCreation(@RequestHeader(HeaderNames.USER_ID) String userId,
                                                             @Valid @RequestBody ProductRequest request) {
        Product product = catalogService.requestCreation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @GetMapping("/products/mine")
    public List<ProductResponse> myRequests(@RequestHeader(HeaderNames.USER_ID) String userId) {
        return catalogService.myRequests(userId).stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.listCategories().stream().map(CategoryResponse::from).toList();
    }
}
