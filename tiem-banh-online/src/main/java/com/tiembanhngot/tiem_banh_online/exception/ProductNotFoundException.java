package com.tiembanhngot.tiem_banh_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Trả về lỗi 404 nếu không tìm thấy sản phẩm
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Product not found")
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}