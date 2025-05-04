package com.tiembanhngot.tiem_banh_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception được ném ra khi không tìm thấy một Order cụ thể.
 * Ánh xạ tới HTTP Status 404 Not Found.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Order not found")
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}