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

    boolean existsByCategoryCategoryId(Integer categoryId);

    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.name ASC")
    List<Product> searchAvailableProducts(@Param("query") String query);
}
