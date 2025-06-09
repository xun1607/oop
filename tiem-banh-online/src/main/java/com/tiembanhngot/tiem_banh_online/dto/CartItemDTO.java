package com.tiembanhngot.tiem_banh_online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    // Class nay dai dien cho 1 ban ghi trong Cart 
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal price; 
    private int quantity;
    private BigDecimal lineTotal; 
    private String selectedSize;
}