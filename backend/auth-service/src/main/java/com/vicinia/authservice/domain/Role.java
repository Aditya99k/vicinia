package com.vicinia.authservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role is modeled as a name plus a set of Permissions — not just a role
 * string — from day one (ARCHITECTURE.md §2, ADR 0006), so that a future
 * SUPPORT_AGENT or MERCHANT_STAFF role is a new row with a scoped
 * permission set, not a schema migration.
 */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void grant(Permission permission) {
        permissions.add(permission);
    }

    public Set<String> permissionNames() {
        return permissions.stream().map(Permission::getName).collect(Collectors.toSet());
    }
}
