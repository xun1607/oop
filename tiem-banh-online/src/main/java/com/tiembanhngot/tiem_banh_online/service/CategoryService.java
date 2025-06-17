package com.tiembanhngot.tiem_banh_online.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.repository.CategoryRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
@Service
@Slf4j
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository; 

    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Category> findAllCategoriesPaginated(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Optional<Category> findById(Integer id) {
        return categoryRepository.findById(id);
    }


    @Transactional
    public Category saveCategory(Category category) {
        boolean isNew = category.getCategoryId() == null;
        log.info("Attempting to save {} category: {}", isNew ? "new" : "existing", category.getName());

        try {
            Category savedCategory = categoryRepository.save(category);
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
            throw new DataIntegrityViolationException("Không thể xóa danh mục ID: " + id + " vì vẫn còn sản phẩm thuộc danh mục này.");
        }

        try {
            categoryRepository.delete(category);
        } catch (DataIntegrityViolationException e) {
            log.error("Cannot delete category ID: {} due to data integrity violation.", id, e);
            throw new DataIntegrityViolationException("Không thể xóa danh mục ID: " + id + " do ràng buộc dữ liệu.");
        }
    }
}