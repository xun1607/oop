package com.tiembanhngot.tiem_banh_online.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final ProductService productService;

    @GetMapping("/search")
    public String searchProducts(
            @RequestParam(value = "query", required = false, defaultValue = "") String query, // Nhận query từ URL
            Model model) {

        log.info("Received search request with query: '{}'", query);
        model.addAttribute("query", query); // Đưa query lại vào model để hiển thị trên trang kết quả

        List<Product> searchResults;
        if (!StringUtils.hasText(query)) {
            searchResults = Collections.emptyList();
            log.debug("Empty search query, returning empty results.");
            model.addAttribute("searchMessage", "Vui lòng nhập từ khóa để tìm kiếm.");
        } else {
            try {
                searchResults = productService.searchAvailableProducts(query);
                log.info("Search returned {} results for query '{}'", searchResults.size(), query);
                if (searchResults.isEmpty()) {
                    model.addAttribute("searchMessage",
                            "Không tìm thấy sản phẩm nào phù hợp với từ khóa '" + query + "'.");
                }
            } catch (Exception e) {
                log.error("Error during product search for query '{}'", query, e);
                searchResults = Collections.emptyList();
                model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình tìm kiếm."); // Thêm lỗi chung
            }
        }

        model.addAttribute("products", searchResults); // Đưa kết quả vào model
        model.addAttribute("currentPage", "search"); // Đặt tên trang cho active link (nếu cần)

        return "search-results"; 
    }
}