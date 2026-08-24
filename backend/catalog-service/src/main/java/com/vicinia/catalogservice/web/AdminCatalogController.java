package com.vicinia.catalogservice.web;

import com.vicinia.common.security.HeaderNames;
import com.vicinia.catalogservice.dto.CategoryRequest;
import com.vicinia.catalogservice.dto.CategoryResponse;
import com.vicinia.catalogservice.dto.ProductRequest;
import com.vicinia.catalogservice.dto.ProductResponse;
import com.vicinia.catalogservice.dto.ReasonRequest;
import com.vicinia.catalogservice.exception.ForbiddenException;
import com.vicinia.catalogservice.service.CatalogService;
import com.vicinia.common.security.PermissionUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Every endpoint here requires the CATALOG_MANAGE permission (seeded onto ADMIN in auth-service's RoleSeeder). */
@RestController
@RequestMapping("/api/catalog/admin")
public class AdminCatalogController {

    private static final String REQUIRED_PERMISSION = "CATALOG_MANAGE";

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                                    @Valid @RequestBody ProductRequest request) {
        require(permissions);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(catalogService.createByAdmin(request)));
    }

    @GetMapping("/products/pending")
    public List<ProductResponse> pending(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions) {
        require(permissions);
        return catalogService.pendingReview().stream().map(ProductResponse::from).toList();
    }

    @PostMapping("/products/{id}/approve")
    public ProductResponse approve(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                    @PathVariable String id) {
        require(permissions);
        return ProductResponse.from(catalogService.approve(id));
    }

    @PostMapping("/products/{id}/reject")
    public ProductResponse reject(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                   @PathVariable String id,
                                   @Valid @RequestBody ReasonRequest request) {
        require(permissions);
        return ProductResponse.from(catalogService.reject(id, request.reason()));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@RequestHeader(HeaderNames.USER_PERMISSIONS) String permissions,
                                                             @Valid @RequestBody CategoryRequest request) {
        require(permissions);
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(catalogService.createCategory(request)));
    }

    private void require(String permissionsHeader) {
        if (!PermissionUtil.hasPermission(permissionsHeader, REQUIRED_PERMISSION)) {
            throw new ForbiddenException("Requires the " + REQUIRED_PERMISSION + " permission");
        }
    }
}
