package com.tiembanhngot.tiem_banh_online.dto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;
import lombok.Getter; 
@Getter
@Data
public class CartDTO {
    private Map<String, CartItemDTO> items = new LinkedHashMap<>();

    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    private int totalItems = 0;

    public Collection<CartItemDTO> getItemList() {
        return items.values();
    }
}