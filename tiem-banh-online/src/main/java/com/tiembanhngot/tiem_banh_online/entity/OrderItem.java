package com.tiembanhngot.tiem_banh_online.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_orderitem_order_id", columnList = "order_id"),
        @Index(name = "idx_orderitem_product_id", columnList = "product_id")
})
@Getter
@Setter
@ToString(exclude = {"order", "product"}) 
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) 
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

   
    @ManyToOne(fetch = FetchType.EAGER, optional = false) //nhieu orderitem nhng chi lien quan toi 1 product (so luong 1 mat hang > 1)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false)
    private Integer quantity; 

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase; 


@Column(name = "size_at_purchase", length = 100)
private String sizeAtPurchase;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return orderItemId != null && Objects.equals(orderItemId, orderItem.orderItemId);
    }

    @Override
    public int hashCode() {
        if(orderItemId != null){
            return Objects.hash(orderItemId);
        }
        else return System.identityHashCode(this);
       
    }
}