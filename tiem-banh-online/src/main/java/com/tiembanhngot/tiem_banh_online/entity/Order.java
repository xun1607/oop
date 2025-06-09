package com.tiembanhngot.tiem_banh_online.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_code", columnList = "order_code", unique = true),
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_created_at", columnList = "created_at") 
})
@Getter
@Setter
@ToString(exclude = {"user", "orderItems"}) 
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", length = 20, unique = true, nullable = false)
    private String orderCode; 

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "user_id") 
    private User user;

    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
    private String shippingAddress;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate; 

    @Column(name = "delivery_time_slot", length = 50)
    private String deliveryTimeSlot; 

    @Column(columnDefinition = "TEXT")
    private String notes; 

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; 

    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO; 

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO; 

    @Column(length = 50, nullable = false)
    private String status = "PENDING"; // trang thai don hang: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; 

    @Column(name = "payment_status", length = 50, nullable = false)
    private String paymentStatus = "UNPAID"; 

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>(); 

    
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }
    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return orderId != null && Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        if(orderId != null){
            return Objects.hash(orderId);
        }
        else return System.identityHashCode(this);
    }
}