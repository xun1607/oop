package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.Objects; // For equals/hashCode
import java.util.Set; // Will be used later for @OneToMany

/**
 * Represents a user of the application.
 */
@Entity
// Add indexes for unique and commonly queried columns
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_phone", columnList = "phone_number", unique = true),
         @Index(name = "idx_user_role_id", columnList = "role_id") // Index for joining with Role
})
@Getter
@Setter
@ToString(exclude = {"passwordHash", "role", "orders"}) // Exclude sensitive/complex fields from default toString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 255, unique = true, nullable = false)
    private String email; // Also used as the username for login

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash; // Stores the hashed password

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber; // Unique phone number

    @ManyToOne(fetch = FetchType.EAGER) // EAGER fetch role as it's often needed with user info
    @JoinColumn(name = "role_id", nullable = false)
    private Role role; // The user's role (e.g., ADMIN, CUSTOMER)

    // Add fields for account status if needed (e.g., isEnabled, isLocked)
    // @Column(name = "is_enabled", nullable = false)
    // private boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Relationship to Orders (will be added later)
    // @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    // private Set<Order> orders;

     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        // Use userId if not null
        return userId != null && Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
         // Use userId if not null
         return userId != null ? Objects.hash(userId) : System.identityHashCode(this);
         // Or simply: return getClass().hashCode(); for persisted entities.
    }
}