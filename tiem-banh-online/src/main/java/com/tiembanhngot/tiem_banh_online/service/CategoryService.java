package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.entity.Category;
// Import exception nếu bạn tạo riêng
// import com.tiembanhngot.tiem_banh_online.exception.CategoryNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.CategoryRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository; // Import để kiểm tra ràng buộc
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

// Tạo exception này nếu chưa có
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
    private final ProductRepository productRepository; // Inject để kiểm tra sản phẩm trước khi xóa

    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll();
    }

    // **Thêm:** Lấy danh sách category có phân trang cho admin list
    @Transactional(readOnly = true)
    public Page<Category> findAllCategoriesPaginated(Pageable pageable) {
        log.debug("Fetching categories with pagination: {}", pageable);
        return categoryRepository.findAll(pageable);
    }


    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(String slug) {
        log.debug("Finding category by slug: {}", slug);
        return categoryRepository.findBySlug(slug);
    }

    // **Thêm:** Tìm category theo ID cho admin edit
    @Transactional(readOnly = true)
    public Optional<Category> findById(Integer id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    // **Thêm/Hoàn thiện:** Lưu category (Thêm mới hoặc Cập nhật)
    @Transactional
    public Category saveCategory(Category category) {
        boolean isNew = category.getCategoryId() == null;
        log.info("Attempting to save {} category: {}", isNew ? "new" : "existing", category.getName());

        // Tự động tạo slug nếu rỗng hoặc là tạo mới
        if (isNew || !StringUtils.hasText(category.getSlug())) {
             String baseSlug = ProductService.generateSlug(category.getName()); // Dùng lại hàm tạo slug
             String finalSlug = baseSlug;
             int counter = 1;
             // Kiểm tra trùng slug và tạo slug duy nhất
             while (categoryRepository.findBySlug(finalSlug).filter(c -> !c.getCategoryId().equals(category.getCategoryId())).isPresent()) {
                 finalSlug = baseSlug + "-" + counter++;
             }
             category.setSlug(finalSlug);
             log.debug("Generated slug for category {}: {}", category.getName(), finalSlug);
        } else {
             // Kiểm tra trùng slug khi cập nhật
             Optional<Category> existingBySlug = categoryRepository.findBySlug(category.getSlug());
             if (existingBySlug.isPresent() && !existingBySlug.get().getCategoryId().equals(category.getCategoryId())) {
                 throw new DataIntegrityViolationException("Slug '" + category.getSlug() + "' đã được sử dụng bởi danh mục khác.");
             }
        }

        try {
            Category savedCategory = categoryRepository.save(category);
            log.info("Category {} successfully with ID: {}", isNew ? "created" : "updated", savedCategory.getCategoryId());
            return savedCategory;
        } catch (DataIntegrityViolationException e) {
             log.error("Data integrity violation while saving category {}: {}", category.getName(), e.getMessage());
              if (e.getMessage() != null && e.getMessage().contains("categories_name_key")) { // Giả sử có unique constraint cho name
                  throw new DataIntegrityViolationException("Tên danh mục '" + category.getName() + "' đã tồn tại.");
              } else if (e.getMessage() != null && e.getMessage().contains("categories_slug_key")) { // Giả sử có unique constraint cho slug
                   throw new DataIntegrityViolationException("Slug '" + category.getSlug() + "' đã tồn tại.");
              }
             throw new RuntimeException("Lỗi khi lưu danh mục: " + e.getMessage(), e); // Ném ra lỗi chung hơn
        }
    }

    // **Thêm/Hoàn thiện:** Xóa category theo ID
    @Transactional
    public void deleteCategoryById(Integer id) {
        log.info("Attempting to delete category with ID: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // **QUAN TRỌNG:** Kiểm tra ràng buộc khóa ngoại trước khi xóa
        if (productRepository.existsByCategoryCategoryId(id)) { // Cần tạo method này trong ProductRepository
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