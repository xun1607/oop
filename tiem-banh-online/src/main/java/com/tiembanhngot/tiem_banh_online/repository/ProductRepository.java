package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategory(Category category);
    List<Product> findByCategoryAndIsAvailableTrue(Category category);
    List<Product> findByIsAvailableTrueOrderByCreatedAtDesc();
    List<Product> findByNameContainingIgnoreCaseAndIsAvailableTrue(String name);

    // **Thêm method này:** Kiểm tra xem có sản phẩm nào thuộc category ID cho trước không
    boolean existsByCategoryCategoryId(Integer categoryId);
}