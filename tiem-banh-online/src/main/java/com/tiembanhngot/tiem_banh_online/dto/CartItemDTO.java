package com.tiembanhngot.tiem_banh_online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long productId;
    private String name;
    private String imageUrl; // Đường dẫn ảnh
    private BigDecimal price; // Đơn giá tại thời điểm thêm
    private int quantity;
    private BigDecimal lineTotal; // Thành tiền (price * quantity)
    private String slug; // Thêm slug để tạo link dễ dàng
}