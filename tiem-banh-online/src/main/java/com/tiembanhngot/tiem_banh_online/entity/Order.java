package com.tiembanhngot.tiem_banh_online.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // Optional
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet; // Import HashSet
import java.util.Objects; // For equals/hashCode
import java.util.Set;

/**
 * Represents a customer order.
 */
@Entity
// Add indexes for commonly queried columns
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_code", columnList = "order_code", unique = true),
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_created_at", columnList = "created_at") // Index for sorting/filtering by date
})
@Getter
@Setter
@ToString(exclude = {"user", "orderItems"}) // Avoid infinite loops/large logs
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", length = 20, unique = true, nullable = false)
    private String orderCode; // Needs logic for generation

    @ManyToOne(fetch = FetchType.LAZY) // LAZY fetch User details unless needed
    @JoinColumn(name = "user_id") // Allow null for guest checkouts
    private User user;

    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
    private String shippingAddress;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate; // Specific date

    @Column(name = "delivery_time_slot", length = 50)
    private String deliveryTimeSlot; // e.g., "9am-12pm"

    @Column(columnDefinition = "TEXT")
    private String notes; // Customer notes

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // Subtotal of items

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO; // Default shipping fee

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO; // Total amount + shipping fee

    @Column(length = 50, nullable = false)
    private String status = "PENDING"; // Default status (e.g., PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // e.g., "COD", "VNPay"

    @Column(name = "payment_status", length = 50, nullable = false)
    private String paymentStatus = "UNPAID"; // Default payment status (e.g., UNPAID, PAID, FAILED)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Relationship: 1 Order - Many OrderItems
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>(); // Initialize set to avoid NullPointerException

    // --- Helper methods for managing bidirectional relationship ---
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
        // Consider recalculating total amounts here or in a service layer
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
        // Consider recalculating total amounts here or in a service layer
    }
    // --- End Helper Methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        // Use orderId if not null (persistent entities), otherwise rely on object identity
        return orderId != null && Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        // Use a fixed value for transient entities, or the ID hashcode for persistent ones
        return orderId != null ? Objects.hash(orderId) : System.identityHashCode(this);
        // Or simply: return getClass().hashCode(); for persisted entities.
    }
}