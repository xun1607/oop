package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
// import com.tiembanhngot.tiem_banh_online.repository.OrderItemRepository; // Bỏ comment nếu dùng
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Đảm bảo import này đúng

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
// import java.util.Collections; // Xóa nếu không dùng searchAvailableProducts

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    private final StorageService storageService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

     @Transactional(readOnly = true)
    public List<Product> searchAvailableProducts(String query) {
        log.debug("Searching for available products with query: '{}'", query);
        if (!StringUtils.hasText(query)) {
            log.warn("Search query is empty or null. Returning empty list.");
            return new ArrayList<>(); // Hoặc List.of();
        }
        String trimmedQuery = query.trim();
        List<Product> results = productRepository.searchAvailableProducts(trimmedQuery); // Gọi đúng repo method
        log.info("Found {} available products matching query: '{}'", results.size(), trimmedQuery);
        return results;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllAvailableProducts() {
        log.debug("Fetching all available products, ordered by creation date desc.");
        return productRepository.findByIsAvailableTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySlug(String slug) {
        log.debug("Finding product by slug: {}", slug);
        return productRepository.findByName(slug);
    }

    // --- Phương thức cho trang Admin ---

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        log.debug("Finding product by ID: {}", id);
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Product> findAllProductsPaginated(Pageable pageable) {
        log.debug("Fetching all products with pagination: {}", pageable);
        return productRepository.findAll(pageable);
    }

    @Transactional
    public Product saveProduct(Product product) { // Chỉ nhận 1 tham số
        boolean isNew = product.getProductId() == null;
        log.info("Attempting to save {} product: ID={}, Name={}",
                 isNew ? "new" : "existing",
                 isNew ? "N/A" : product.getProductId(),
                 product.getName());

        // 1. Xử lý và kiểm tra Slug
        if (!StringUtils.hasText(product.getName())) {
             throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }

        // 2. ImageUrl đã được xử lý ở Controller, không cần xử lý lại ở đây

        // 3. Lưu sản phẩm vào DB
        try {
            // === DI CHUYỂN DÒNG LOG VÀO ĐÂY ===
            log.debug("Saving product with imageUrl: {}", product.getImageUrl());
            Product savedProduct = productRepository.save(product);
            // === KẾT THÚC DI CHUYỂN ===
            log.info("Product {} successfully with ID: {}", isNew ? "created" : "updated", savedProduct.getProductId());
            return savedProduct;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving product {}: {}", product.getName(), e.getMessage());
            if (e.getMessage() != null) {
                 String lowerCaseMsg = e.getMessage().toLowerCase();
                 if (lowerCaseMsg.contains("name") || lowerCaseMsg.contains("products_name_key")) {
                      throw new DataIntegrityViolationException("Tên sản phẩm '" + product.getName() + "' đã tồn tại.", e);
                 }
            }
            throw new RuntimeException("Lỗi khi lưu sản phẩm: " + e.getMessage(), e); // Ném lỗi chung hơn
        }
    }



    @Transactional
    public void deleteProductById(Long id) {
        log.info("Attempting to delete product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id + " để xóa."));

        // Kiểm tra ràng buộc khóa ngoại (Bỏ comment và triển khai nếu cần)
        // if (orderItemRepository.existsByProductProductId(id)) {
        //     log.warn("Attempted to delete product ID: {} which exists in orders.", id);
        //     throw new DataIntegrityViolationException("Không thể xóa sản phẩm ID: " + id + " vì đã tồn tại trong đơn hàng.");
        // }

        String imageUrlToDelete = product.getImageUrl();

        try {
            productRepository.delete(product);
            log.info("Successfully deleted product entity ID: {}", id);

            if (StringUtils.hasText(imageUrlToDelete) && imageUrlToDelete.startsWith("/uploads/")) {
                 try {
                     String filenameToDelete = imageUrlToDelete.substring("/uploads/".length());
                     storageService.delete(filenameToDelete);
                     log.info("Successfully deleted associated image file: {}", filenameToDelete);
                 } catch (IOException e) {
                      log.error("Could not delete image file {} for deleted product ID {}", imageUrlToDelete, id, e);
                 }
             }
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete product ID: {} due to data integrity violation.", id, e);
             throw new DataIntegrityViolationException("Không thể xóa sản phẩm ID: " + id + " do ràng buộc dữ liệu (có thể do đơn hàng).", e);
        }
    }

    public static String generateSlug(String input) {
        if (!StringUtils.hasText(input)) { // Dùng StringUtils cho nhất quán
            return "san-pham-" + System.currentTimeMillis();
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("[đĐ]", "d");
        String temp = NONLATIN.matcher(normalized).replaceAll("");
        String slug = WHITESPACE.matcher(temp).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH)
                   .replaceAll("[^a-z0-9\\-]", "")
                   .replaceAll("-{2,}", "-")
                   .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "san-pham-" + System.currentTimeMillis() : slug;
    }
}