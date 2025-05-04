package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional

import java.math.BigDecimal;
import java.util.Objects; // For equals/hashCode

/**
 * Represents a single item within an Order.
 */
@Entity
// Add indexes for foreign keys often used in joins/queries
@Table(name = "order_items", indexes = {
        @Index(name = "idx_orderitem_order_id", columnList = "order_id"),
        @Index(name = "idx_orderitem_product_id", columnList = "product_id")
})
@Getter
@Setter
@ToString(exclude = {"order", "product"}) // Avoid potential issues with lazy loading/loops
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    // Relationship: Many OrderItems - 1 Order
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Should always belong to an Order
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Relationship: Many OrderItems - 1 Product
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Should always refer to a Product
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // Reference to the actual product

    @Column(nullable = false)
    private Integer quantity; // Number of this product ordered

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase; // Stores the price when the order was placed

    @Column(name = "size_at_purchase", length = 50) // Example: if products have sizes
    private String sizeAtPurchase; // Stores the size when the order was placed

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        // Use orderItemId if not null (persistent entity)
        return orderItemId != null && Objects.equals(orderItemId, orderItem.orderItemId);
    }

    @Override
    public int hashCode() {
         // Use orderItemId for hash code if not null
        return orderItemId != null ? Objects.hash(orderItemId) : System.identityHashCode(this);
        // Or simply: return getClass().hashCode(); for persisted entities.
    }
}