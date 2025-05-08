package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    

    // === THÊM PHƯƠNG THỨC TÌM KIẾM ===
    /**
     * Tìm kiếm sản phẩm có sẵn theo tên hoặc mô tả (không phân biệt hoa thường).
     * @param nameQuery Từ khóa tìm kiếm cho tên.
     * @param descriptionQuery Từ khóa tìm kiếm cho mô tả.
     * @return Danh sách sản phẩm phù hợp và isAvailable = true.
     */
    List<Product> findByIsAvailableTrueAndNameContainingIgnoreCaseOrIsAvailableTrueAndDescriptionContainingIgnoreCaseOrderByNameAsc(String nameQuery, String descriptionQuery);
    // Lưu ý: Tên phương thức dài nhưng mô tả rõ ràng query Spring Data JPA sẽ tạo ra:
    // Tìm ( (isAvailable=true AND name LIKE %nameQuery%) OR (isAvailable=true AND description LIKE %descriptionQuery%) ) ORDER BY name ASC
    // === KẾT THÚC PHẦN THÊM ===
    // === THÊM TRUY VẤN TÌM KIẾM ===
    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND " +
           "(LOWER(p.name) LIKE LOWER(concat('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(concat('%', :query, '%'))) " +
           "ORDER BY p.name ASC")
    List<Product> searchAvailableProducts(@Param("query") String query);
    // === KẾT THÚC PHẦN THÊM ===

}