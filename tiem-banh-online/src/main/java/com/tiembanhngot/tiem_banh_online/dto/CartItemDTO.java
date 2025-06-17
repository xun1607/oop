package com.tiembanhngot.tiem_banh_online.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal price; 
    private int quantity;
    private BigDecimal lineTotal; 
    private String selectedSize;
}