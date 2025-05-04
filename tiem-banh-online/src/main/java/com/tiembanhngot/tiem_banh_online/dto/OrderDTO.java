package com.tiembanhngot.tiem_banh_online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
// Thêm các import cần thiết khác (LocalDate, etc.)

@Data
public class OrderDTO {
    // Thông tin người nhận
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    private String notes;

    // Thông tin giao hàng (Tùy chọn)
    // private LocalDate deliveryDate;
    // private String deliveryTimeSlot;

    // Phương thức thanh toán
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod; // Ví dụ: "COD", "VNPAY"

    // Các trường khác nếu cần từ form checkout
}