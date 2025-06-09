package com.tiembanhngot.tiem_banh_online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;  
import lombok.Data;                            

@Data
public class OrderDTO {

    @NotBlank(message = "Recipient's name cannot be empty")
    private String recipientName;

    @NotBlank(message = "Recipient's phone number cannot be empty")
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "Invalid phone number")
    private String recipientPhone;

    @NotBlank(message = "Shipping address cannot be empty")
    private String shippingAddress;

    private String notes;
    private String paymentMethod;
}