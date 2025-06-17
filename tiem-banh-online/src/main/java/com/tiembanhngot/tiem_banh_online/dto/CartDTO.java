package com.tiembanhngot.tiem_banh_online.dto;

import lombok.Data;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.LinkedHashMap; 
import java.util.Map;
import java.util.Collection; 
@Getter
@Data
public class CartDTO {
    // Class dai dien cho 1 Cart tam thoi
    // LinkedHasdMap de duy tri thu tu trong Cart
    private Map<String, CartItemDTO> items = new LinkedHashMap<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    private int totalItems = 0; 

    public Collection<CartItemDTO> getItemList() {
        return items.values();
    }
}