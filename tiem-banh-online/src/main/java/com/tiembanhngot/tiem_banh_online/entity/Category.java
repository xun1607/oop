package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional: for debugging

import java.util.Objects; // For equals/hashCode
// import java.util.Set; // Will be used later for @OneToMany

/**
 * Represents a product category (e.g., Banh Kem, Pastry).
 */
@Entity
// Add index for the frequently queried 'slug' column
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug", unique = true)
})
@Getter
@Setter
@ToString(exclude = "products") // Avoid potential issues if products relationship is added later
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(length = 100, unique = true, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 120, unique = true, nullable = false)
    private String slug; // URL-friendly identifier

    // Relationship to Products (will be added later)
    // @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    // private Set<Product> products;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        // Use categoryId for equality check if it's not null (persistent entity)
        return categoryId != null && Objects.equals(categoryId, category.categoryId);
    }

    @Override
    public int hashCode() {
         // Use categoryId for hash code if it's not null, otherwise use object's identity hash code
         return categoryId != null ? Objects.hash(categoryId) : System.identityHashCode(this);
         // Or simply: return getClass().hashCode(); for persisted entities. Be consistent with equals.
    }
}