package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.service.CategoryService;
// Import exception nếu tạo riêng
import com.tiembanhngot.tiem_banh_online.exception.CategoryNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryController {

    private final CategoryService categoryService;

    // Hiển thị danh sách danh mục (phân trang, sắp xếp)
    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "categoryId,asc") String sort,
            Model model) {

        log.info("Request received for category list: page={}, size={}, sort={}", page, size, sort);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.ASC;
        String sortField = sortParams[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Category> categoryPage = categoryService.findAllCategoriesPaginated(pageable);

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);

        model.addAttribute("currentPage", "categories"); // <-- THÊM DÒNG NÀY
    log.debug("Returning category list view...");
    return "admin/category/list";
    }

    // Hiển thị form thêm danh mục
    @GetMapping("/add")
    public String showAddCategoryForm(Model model) {
        log.debug("Request received for add category form.");
        model.addAttribute("category", new Category()); // Category rỗng cho form
        model.addAttribute("pageTitle", "Thêm Danh Mục Mới");
        return "admin/category/form"; // --> /templates/admin/category/form.html
    }

    // Hiển thị form sửa danh mục
    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Request received to edit category with ID: {}", id);
        try {
            Category category = categoryService.findById(id)
                    .orElseThrow(() -> new CategoryNotFoundException("Không tìm thấy danh mục với ID: " + id));
            model.addAttribute("category", category);
            model.addAttribute("pageTitle", "Chỉnh Sửa Danh Mục (ID: " + id + ")");
            log.debug("Displaying edit form for category ID: {}", id);
            return "admin/category/form";
        } catch (CategoryNotFoundException e) {
            log.warn("Category not found for editing: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    // Xử lý lưu (thêm mới hoặc cập nhật) danh mục
    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("category") Category category,
                               BindingResult bindingResult,
                               Model model, // Để trả về form nếu lỗi
                               RedirectAttributes redirectAttributes) {

        log.info("Attempting to save category: {}", category.getName() != null ? category.getName() : "New Category");
        boolean isNew = category.getCategoryId() == null;
        String pageTitle = isNew ? "Thêm Danh Mục Mới" : "Chỉnh Sửa Danh Mục (ID: " + category.getCategoryId() + ")";
        model.addAttribute("pageTitle", pageTitle); // Set lại title phòng khi lỗi

        // Kiểm tra lỗi validation cơ bản
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors found for category {}: {}", category.getName(), bindingResult.getAllErrors());
            return "admin/category/form"; // Trả về form nếu có lỗi
        }

        // Lưu danh mục
        try {
            log.debug("Calling categoryService.saveCategory for category: {}", category.getName());
            categoryService.saveCategory(category);
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + (isNew ? "thêm" : "cập nhật") + " danh mục thành công!");
            log.info("Category {} successfully.", isNew ? "created" : "updated");
            return "redirect:/admin/categories";
        } catch (DataIntegrityViolationException e) { // Bắt lỗi trùng tên/slug
            log.error("Data integrity violation while saving category {}: {}", category.getName(), e.getMessage());
            // Phân tích lỗi để hiển thị đúng field (cần dựa vào thông báo lỗi DB hoặc constraint name)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("slug")) {
                 bindingResult.rejectValue("slug", "duplicate.slug", e.getMessage());
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("name")) {
                 bindingResult.rejectValue("name", "duplicate.name", e.getMessage());
            } else {
                 model.addAttribute("errorMessage", "Lỗi lưu danh mục: " + e.getMessage());
            }
            return "admin/category/form";
        } catch (Exception e) {
            log.error("Error saving category {}", category.getName(), e);
            model.addAttribute("errorMessage", "Lỗi không mong muốn khi lưu danh mục: " + e.getMessage());
             model.addAttribute("category", category); // Đảm bảo category object còn trong model
            return "admin/category/form"; // Quay lại form với thông báo lỗi
        }
    }

    // Xử lý xóa danh mục
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        log.info("Request received to delete category with ID: {}", id);
        try {
            categoryService.deleteCategoryById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục ID: " + id);
            log.info("Successfully deleted category ID: {}", id);
        } catch (CategoryNotFoundException e) {
             log.warn("Attempted to delete non-existent category: {}", e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) { // Bắt lỗi ràng buộc khóa ngoại
            log.error("Cannot delete category ID: {} due to existing references (e.g., products).", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // Hiển thị lỗi từ service
        } catch (Exception e) {
            log.error("Error deleting category ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không mong muốn khi xóa danh mục ID: " + id);
        }
        return "redirect:/admin/categories";
    }
}