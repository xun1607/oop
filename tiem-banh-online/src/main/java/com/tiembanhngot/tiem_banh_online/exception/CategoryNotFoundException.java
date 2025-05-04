package com.tiembanhngot.tiem_banh_online.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception được ném ra khi không tìm thấy một Category cụ thể.
 * Có thể được ánh xạ tự động tới HTTP Status 404 Not Found
 * nếu được ném ra từ một Controller hoặc không được bắt lại.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Category not found") // Optional: Tự động trả về 404 nếu không bị bắt
public class CategoryNotFoundException extends RuntimeException {

    /**
     * Constructor với thông báo lỗi tùy chỉnh.
     * @param message Thông báo mô tả chi tiết lỗi.
     */
    public CategoryNotFoundException(String message) {
        super(message); // Gọi constructor của lớp cha (RuntimeException)
    }

    /**
     * Constructor với thông báo lỗi và nguyên nhân gốc (cause).
     * @param message Thông báo mô tả chi tiết lỗi.
     * @param cause Exception gốc gây ra lỗi này.
     */
    public CategoryNotFoundException(String message, Throwable cause) {
        super(message, cause); // Gọi constructor của lớp cha
    }
}