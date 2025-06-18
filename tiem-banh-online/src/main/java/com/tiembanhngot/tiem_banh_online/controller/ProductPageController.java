package com.tiembanhngot.tiem_banh_online.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.service.ProductService;

import lombok.extern.slf4j.Slf4j;



@Controller
@RequestMapping("/products")
@Slf4j
public class ProductPageController {
    @Autowired
    private ProductService productService;

    // @GetMapping
    // public String listProducts(Model model) { 
    //     List<Product> products = productService.findAllAvailableProducts();
    //     model.addAttribute("products", products);
    //     model.addAttribute("currentPage", "products");
    //     log.info("Rendering product list page with {} products", products.size());
    //     return "product/list";
    // }
    @GetMapping
    public String listProductsByCategory(Model model) {
        Map<Category, List<Product>> productsByCategory = productService.getProductsGroupedByCategory();

        model.addAttribute("productsByCategory", productsByCategory);
        model.addAttribute("currentPage", "products");
        return "product/list";
    }


    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        model.addAttribute("currentPage", "products");
        Product product = productService.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product not found with ID: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm với ID: " + id);
                });
        model.addAttribute("product", product);
        log.debug("Rendering product detail page for product: {}", product.getName());
        return "product/detail";
    }
}