package com.tiembanhngot.tiem_banh_online.service;


import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    private final Path root = Paths.get("uploads");

    public ProductService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục uploads.", e);
        }
    }

    public long countTotalProducts(){
        return productRepository.count();
    }
    
    public List<Product> searchAvailableProducts(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return productRepository.searchAvailableProducts(query.trim());
    }

    public List<Product> findAllAvailableProducts() {
        return productRepository.findByIsAvailableTrueOrderByCreatedAtDesc();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }


    public Page<Product> findAllProductsPaginated(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public Product saveProduct(Product product, MultipartFile image) throws IOException {
        if (!StringUtils.hasText(product.getName())) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }

        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Files.copy(image.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            product.setImageUrl("/uploads/" + fileName);
        }

        try {
            return productRepository.save(product);
            } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("name")) {
                throw new DataIntegrityViolationException("Tên sản phẩm đã tồn tại.", e);
            }
            throw new RuntimeException("Lỗi khi lưu sản phẩm.", e);
            }
    }
    

    public Product updateProduct(Product product, MultipartFile image) throws IOException {
        Product existing = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm để cập nhật."));

        if (image != null && !image.isEmpty()) {
            // Xoá ảnh cũ nếu có
            if (StringUtils.hasText(existing.getImageUrl())) {
                Path oldImage = root.resolve(existing.getImageUrl().replace("/uploads/", ""));
                Files.deleteIfExists(oldImage);
            }
            // Lưu ảnh mới
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Files.copy(image.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            product.setImageUrl("/uploads/" + fileName);
        } else {
            product.setImageUrl(existing.getImageUrl());
        }
        return productRepository.save(product);
    }

    public void deleteProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        try {
            productRepository.delete(product);

            if (StringUtils.hasText(product.getImageUrl())) {
                Path imagePath = root.resolve(product.getImageUrl().replace("/uploads/", ""));
                Files.deleteIfExists(imagePath);
            }

        } catch (IOException e) {
            System.err.println("Không thể xoá ảnh: " + e.getMessage());
            
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityViolationException("Không thể xóa sản phẩm vì ràng buộc dữ liệu.", e);
        }
    }

    public Map<Category, List<Product>> getProductsGroupedByCategory() {
        List<Product> products = findAllAvailableProducts();

        Map<Category, List<Product>> grouped = products.stream()
            .filter(p -> p.getCategory() != null)
            .collect(Collectors.groupingBy(Product::getCategory));

        return grouped.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getKey().getCategoryId()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }


}

