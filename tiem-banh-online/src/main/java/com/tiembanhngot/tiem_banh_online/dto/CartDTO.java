package com.tiembanhngot.tiem_banh_online.dto;

import lombok.Data;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.LinkedHashMap; // **Dùng LinkedHashMap để giữ thứ tự thêm vào**
import java.util.Map;
import java.util.Collection; // Import Collection
@Getter
@Data
public class CartDTO {
    // Dùng Map<ProductId, CartItemDTO> để truy cập item nhanh hơn
    private Map<String, CartItemDTO> items = new LinkedHashMap<>();
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private int totalItems = 0; // Tổng số lượng các sản phẩm

    // Helper method để lấy danh sách items cho Thymeleaf dễ lặp
    public Collection<CartItemDTO> getItemList() {
        return items.values();
    }
}