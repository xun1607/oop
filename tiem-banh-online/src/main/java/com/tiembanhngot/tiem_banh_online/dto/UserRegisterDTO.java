package com.tiembanhngot.tiem_banh_online.dto;

import jakarta.validation.constraints.*; 
import lombok.Data;               

@Data 
public class UserRegisterDTO {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email is not in a valid format")
    @Size(max = 255, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Confirm password cannot be empty")
    private String confirmPassword; 

    @NotBlank(message = "Full name cannot be empty")
    @Size(max = 100, message = "Full name is too long")
    private String fullName;

    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Phone number is invalid")
    @Size(max = 20, message = "Phone number is too long") 
    private String phoneNumber;
}