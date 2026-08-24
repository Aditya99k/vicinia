package com.vicinia.inventoryservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local read-model of catalog-service products, populated by
 * ProductEventConsumer on product.created. catalog-service's event payload
 * carries only a productId (no name/category), so this cache is built by a
 * single follow-up REST fetch per product — after that, every listing
 * created for the same product reads this local row instead of calling
 * catalog-service again. A miss here (event not consumed yet, or predates
 * this service's first boot before backfill completes) falls back to a
 * direct REST call — see CatalogClient and InventoryService.resolveProduct.
 */
@Entity
@Table(name = "known_products")
public class KnownProduct {

    @Id
    private String id;

    private String name;
    private String category;
    private Instant syncedAt = Instant.now();

    protected KnownProduct() {
    }

    public KnownProduct(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }
}
