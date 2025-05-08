package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional

import java.util.Objects; // For equals/hashCode
import java.util.Set; // Will be used later for @OneToMany

/**
 * Represents a user role (e.g., ADMIN, CUSTOMER).
 */
@Entity
// Add index for role_name as it's unique and used for lookups
@Table(name = "roles", indexes = {
        @Index(name = "idx_role_name", columnList = "role_name", unique = true)
})
@Getter
@Setter
@ToString(exclude = "users") // Avoid issues if users relationship is added later
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", length = 50, unique = true, nullable = false)
    private String roleName; // e.g., "admin", "customer"

    // Relationship to Users (will be added later)
    // @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    // private Set<User> users;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        // Use roleId if not null
        return roleId != null && Objects.equals(roleId, role.roleId);
    }

    @Override
    public int hashCode() {
         // Use roleId if not null
         return roleId != null ? Objects.hash(roleId) : System.identityHashCode(this);
         // Or simply: return getClass().hashCode(); for persisted entities.
    }
}