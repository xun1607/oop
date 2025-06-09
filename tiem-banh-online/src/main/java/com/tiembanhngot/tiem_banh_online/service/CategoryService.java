package com.tiembanhngot.tiem_banh_online.service;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.repository.CategoryRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository; // Import để kiểm tra ràng buộc

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}


@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; 

    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Category> findAllCategoriesPaginated(Pageable pageable) {
        log.debug("Fetching categories with pagination: {}", pageable);
        return categoryRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Optional<Category> findById(Integer id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }

  
    @Transactional
    public Category saveCategory(Category category) {
        boolean isNew = category.getCategoryId() == null;
        log.info("Attempting to save {} category: {}", isNew ? "new" : "existing", category.getName());

        try {
            Category savedCategory = categoryRepository.save(category);
            log.info("Category {} successfully with ID: {}", isNew ? "created" : "updated", savedCategory.getCategoryId());
            return savedCategory;
        } catch (DataIntegrityViolationException e) {
        log.error("Data integrity violation while saving category '{}': {}", category.getName(), e.getMessage());

        if (e.getMessage() != null && e.getMessage().contains("categories_name_key")) {
            throw new DataIntegrityViolationException("Tên danh mục '" + category.getName() + "' đã tồn tại. Vui lòng chọn một tên khác.");
            }
        throw new RuntimeException("Lỗi hệ thống khi đang cố gắng lưu danh mục.", e);
        }
        
    }

    
    @Transactional
    public void deleteCategoryById(Integer id) {
        log.info("Attempting to delete category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục với ID: " + id));

        if (productRepository.existsByCategoryCategoryId(id)) { 
            log.warn("Attempted to delete category ID: {} which still has associated products.", id);
            throw new DataIntegrityViolationException("Không thể xóa danh mục ID: " + id + " vì vẫn còn sản phẩm thuộc danh mục này.");
        }

        try {
             categoryRepository.delete(category);
             log.info("Successfully deleted category ID: {}", id);
        } catch (DataIntegrityViolationException e) { // Trường hợp có ràng buộc khác
            log.error("Cannot delete category ID: {} due to data integrity violation.", id, e);
             throw new DataIntegrityViolationException("Không thể xóa danh mục ID: " + id + " do ràng buộc dữ liệu.");
        }
    }
}