package com.tiembanhngot.tiem_banh_online.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Import Model
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.service.ProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductPageController {

    private final ProductService productService;

    @GetMapping
    public String listProducts(Model model) { // Thêm Model
        model.addAttribute("currentPage", "products"); // Đặt tên trang
        List<Product> products = productService.findAllAvailableProducts();
        model.addAttribute("products", products);
        return "product/list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable("id") Long productId, Model model) { // Thêm Model
         model.addAttribute("currentPage", "products"); // Trang chi tiết vẫn thuộc mục sản phẩm
        Product product = productService.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với id: " + productId));
        model.addAttribute("product", product);
        return "product/detail";
    }
}