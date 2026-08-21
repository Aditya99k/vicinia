package com.vicinia.authservice.service;

import com.vicinia.authservice.domain.Permission;
import com.vicinia.authservice.domain.Role;
import com.vicinia.authservice.repository.PermissionRepository;
import com.vicinia.authservice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Seeds the 4 V1 roles and their permission bundles on first boot
 * (ARCHITECTURE.md §2). Modeling this as role -> permission-set from day
 * one is what makes a future SUPPORT_AGENT or MERCHANT_STAFF role a new
 * seeded row instead of a schema migration (ADR 0006).
 */
@Component
public class RoleSeeder implements CommandLineRunner {

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "CUSTOMER", List.of("ORDER_PLACE", "ORDER_VIEW_OWN", "PROFILE_MANAGE"),
            "MERCHANT", List.of("STORE_MANAGE", "LISTING_MANAGE", "ORDER_FULFILL", "PROFILE_MANAGE"),
            "DELIVERY_PARTNER", List.of("DELIVERY_MANAGE", "PROFILE_MANAGE"),
            "ADMIN", List.of("MERCHANT_APPROVE", "USER_MANAGE", "COUPON_MANAGE",
                    "ORDER_VIEW_ALL", "PAYMENT_VIEW", "REFUND_INITIATE")
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            return;
        }
        ROLE_PERMISSIONS.forEach((roleName, permissionNames) -> {
            Role role = new Role(roleName);
            for (String permissionName : permissionNames) {
                Permission permission = permissionRepository.findByName(permissionName)
                        .orElseGet(() -> permissionRepository.save(new Permission(permissionName)));
                role.grant(permission);
            }
            roleRepository.save(role);
        });
    }
}
