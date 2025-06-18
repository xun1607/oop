package com.tiembanhngot.tiem_banh_online.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.service.ProductService;


@Controller
@RequestMapping("/products")
public class ProductPageController {
    @Autowired
    private ProductService productService;

    @GetMapping
    public String listProducts(Model model) { // Thêm Model
        List<Product> products = productService.findAllAvailableProducts();
        model.addAttribute("products", products);
        model.addAttribute("currentPage", "products");
        return "product/list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "products");
        Product product = productService.findById(id)
                .orElseThrow(() -> {
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id);
                });
        model.addAttribute("product", product);
        return "product/detail";
    }
}