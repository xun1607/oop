package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects; // For equals/hashCode
import java.util.List; // For gallery_urls later
import java.util.Map; // For size_options later

/**
 * Represents a product available for sale.
 */
@Entity
// Add indexes for commonly queried/filtered/sorted columns
@Table(name = "products", indexes = {
        @Index(name = "idx_product_slug", columnList = "slug", unique = true),
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_is_available", columnList = "is_available"),
         @Index(name = "idx_product_created_at", columnList = "created_at") // For sorting by newest
})
@Getter
@Setter
@ToString(exclude = "category") // Avoid issues with lazy loading Category
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Base price

    // Future enhancements:
    //@Column(name = "size_options", columnDefinition = "jsonb")
    // @Type(JsonBinaryType.class) // Requires hypersistence-utils dependency
    //private Map<String, BigDecimal> sizeOptions; // e.g., {"small": 300000, "large": 450000}

    @Column(name = "image_url", length = 500)
    private String imageUrl; // Main representative image URL

    // Future enhancements:
    // @Column(name = "gallery_urls", columnDefinition = "text[]") // PostgreSQL specific array
    // @Type(ListArrayType.class) // Requires hypersistence-utils dependency
    // private List<String> galleryUrls; // URLs for additional images

    @ManyToOne(fetch = FetchType.LAZY) // LAZY fetch category details unless needed
    @JoinColumn(name = "category_id") // Foreign key column in 'products' table
    private Category category;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true; // Default value, indicates if product is for sale

    @Column(length = 270, unique = true, nullable = false) // Length slightly > name for suffixes if needed
    private String slug; // URL-friendly unique identifier

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        // Use productId if not null
        return productId != null && Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
         // Use productId if not null
        return productId != null ? Objects.hash(productId) : System.identityHashCode(this);
        // Or simply: return getClass().hashCode(); for persisted entities.
    }
}