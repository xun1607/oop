package com.tiembanhngot.tiem_banh_online.dto;

import jakarta.validation.constraints.*; // Import các annotation validation
import lombok.Data;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên quá dàia")
    private String fullName;

    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ") // Regex cơ bản cho SĐT VN
    @Size(max = 20, message = "Số điện thoại quá dài")
    private String phoneNumber;
}