package com.tiembanhngot.tiem_banh_online.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects; 
import java.util.HashMap;
import java.util.Map; 

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category_id", columnList = "category_id")
})
@ToString(exclude = {"category", "sizeOptions"}) 
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price; 

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Type(JsonBinaryType.class) 
    @Column(name = "size_options", columnDefinition = "jsonb") 
    private Map<String, BigDecimal> sizeOptions = new HashMap<>(); 
   
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "category_id") 
    private Category category;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true; 

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
        return productId != null && Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return (productId != null) ? productId.hashCode() : 0;
    }
}
