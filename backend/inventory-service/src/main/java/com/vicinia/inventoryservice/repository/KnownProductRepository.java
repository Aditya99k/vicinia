package com.vicinia.inventoryservice.repository;

import com.vicinia.inventoryservice.domain.KnownProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnownProductRepository extends JpaRepository<KnownProduct, String> {
}
