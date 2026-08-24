package com.vicinia.catalogservice.service;

import com.vicinia.catalogservice.domain.Category;
import com.vicinia.catalogservice.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A reasonable default category set so the catalog isn't empty on first
 * boot — matches the six category glyphs already built into
 * frontend/customer-app's home page (Stage 3's "Shop by category" section
 * was honest placeholder decoration at the time; these are now real).
 */
@Component
public class CategorySeeder implements CommandLineRunner {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Fruits & Vegetables", "Dairy & Eggs", "Bakery", "Snacks",
            "Beverages", "Personal Care", "Household Essentials", "Atta, Rice & Dal"
    );

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        for (String name : DEFAULT_CATEGORIES) {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                categoryRepository.save(new Category(name, slugify(name)));
            }
        }
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
