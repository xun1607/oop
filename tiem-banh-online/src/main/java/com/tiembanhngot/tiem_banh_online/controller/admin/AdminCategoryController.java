package com.tiembanhngot.tiem_banh_online.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.exception.CategoryNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.CategoryService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminCategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "categoryId,asc") String sort, // sap xep theo categoryId, tang dan
            Model model) {


        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.ASC;
        String sortField = sortParams[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField)); //hien thi danh sach tren nhieu trang -> ko qua tai du lieu

        Page<Category> categoryPage = categoryService.findAllCategoriesPaginated(pageable);

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);

        model.addAttribute("currentPage", "categories"); 
    
    return "admin/category/list";
    }

    // thêm category
    @GetMapping("/add")
    public String showAddCategoryForm(Model model) {
        model.addAttribute("category", new Category()); 
        model.addAttribute("pageTitle", "Thêm Danh Mục Mới");
        return "admin/category/form";
    }

    // sửa category
    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
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
                            Model model,
                            RedirectAttributes redirectAttributes) {

        boolean isNew = category.getCategoryId() == null;
        String pageTitle = isNew ? "Thêm Danh Mục Mới" : "Chỉnh Sửa Danh Mục (ID: " + category.getCategoryId() + ")";
        model.addAttribute("pageTitle", pageTitle);

        // Kiểm tra lỗi validation cơ bản
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors found for category {}: {}", category.getName(), bindingResult.getAllErrors());
            return "admin/category/form";
        }

        try {
            categoryService.saveCategory(category); //lưu category (tạo mới hoặc sửa cũ)
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + (isNew ? "thêm" : "cập nhật") + " danh mục thành công!");
            log.info("Category {} successfully.", isNew ? "created" : "updated");
            return "redirect:/admin/categories";
        } catch (DataIntegrityViolationException e) { // Bắt lỗi trùng tên
            log.error("Data integrity violation while saving category {}: {}", category.getName(), e.getMessage());
            if(e.getMessage() != null && e.getMessage().toLowerCase().contains("name")) {
                bindingResult.rejectValue("name", "duplicate.name", e.getMessage());
            } else {
                model.addAttribute("errorMessage", "Lỗi lưu danh mục: " + e.getMessage());
            }
            return "admin/category/form";
        } catch (Exception e) {
            log.error("Error saving category {}", category.getName(), e);
            model.addAttribute("errorMessage", "Lỗi không mong muốn khi lưu danh mục: " + e.getMessage());
            model.addAttribute("category", category);
            return "admin/category/form";
        }
    }

    // xóa danh mục
    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategoryById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục ID: " + id);
    
        } catch (CategoryNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) { // Bắt lỗi ràng buộc khóa ngoại
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // Hiển thị lỗi từ service
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không mong muốn khi xóa danh mục ID: " + id);
        }
        return "redirect:/admin/categories";
    }
}