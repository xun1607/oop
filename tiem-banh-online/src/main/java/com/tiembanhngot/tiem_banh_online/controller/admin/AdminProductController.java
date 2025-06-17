package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.CategoryService;
import com.tiembanhngot.tiem_banh_online.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;


    private void loadCategories(Model model) {
        model.addAttribute("categories", categoryService.findAllCategories());
    }

    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "productId,asc") String sort,
            Model model) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Page<Product> productPage = productService.findAllProductsPaginated(pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", "products");
        model.addAttribute("sortField", sortParams[0]);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("pageSize", size);
        return "admin/product/list";
    }

    // Hiển thị form thêm
    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
        loadCategories(model);
        return "admin/product/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
            model.addAttribute("product", product);
            model.addAttribute("pageTitle", "Chỉnh Sửa Sản Phẩm (ID: " + id + ")");
            loadCategories(model);
            return "admin/product/form";
        } catch (ProductNotFoundException e) {
            log.warn("Admin: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }


    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm ID: " + id);
        } catch (ProductNotFoundException e) {
            log.warn("Admin: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            log.error("Admin: Không thể xóa sản phẩm ID {} do ràng buộc dữ liệu", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm vì ràng buộc dữ liệu.");
        } catch (Exception e) {
            log.error("Admin: Lỗi không mong muốn khi xóa sản phẩm ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm.");
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/save")
    public String saveProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "currentImageUrl", required = false) String currentImageUrl,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean isNew = (product.getProductId() == null);
        String pageTitle = isNew ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")";
        model.addAttribute("pageTitle", pageTitle);
        // Kiểm tra danh mục
        if (product.getCategory() == null || product.getCategory().getCategoryId() == null) {
            bindingResult.rejectValue("category", "NotEmpty.product.category", "Vui lòng chọn danh mục.");
        }

        if (bindingResult.hasErrors()) {
            loadCategories(model);
            if (!isNew) {
                product.setImageUrl(currentImageUrl);
            }
            return "admin/product/form";
        }

        try {
            // Dùng ProductService để xử lý upload ảnh và lưu DB
            productService.saveProduct(product, imageFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    (isNew ? "Đã thêm" : "Đã cập nhật") + " sản phẩm '" + product.getName() + "' thành công!");
            return "redirect:/admin/products";

<<<<<<< HEAD
        } catch (DataIntegrityViolationException e) { // Lỗi ràng buộc dữ liệu (ví dụ: slug trùng)
            log.error("Data integrity violation while saving product '{}': {}", product.getName(), e.getMessage());
            // Cố gắng xác định lỗi và thêm vào BindingResult
            // Lưu ý: Tên constraint ('products_slug_key', 'products_name_key') cần khớp với tên trong DB của bạn
            if (e.getMessage() != null) {
                String lowerCaseMsg = e.getMessage().toLowerCase();
                if (lowerCaseMsg.contains("slug") || lowerCaseMsg.contains("products_slug_key")) { // Kiểm tra cả text và tên constraint (ví dụ)
                     bindingResult.rejectValue("slug", "duplicate.slug", "Slug '" + product.getSlug() + "' đã tồn tại. Vui lòng sửa tên sản phẩm hoặc slug.");
                } else if (lowerCaseMsg.contains("name") || lowerCaseMsg.contains("products_name_key")) { // Ví dụ nếu có unique constraint cho name
                     bindingResult.rejectValue("name", "duplicate.name", "Tên sản phẩm '" + product.getName() + "' đã tồn tại.");
                } else {
                     // Lỗi ràng buộc khác không xác định
                     model.addAttribute("errorMessage", "Lỗi lưu sản phẩm do ràng buộc dữ liệu: " + e.getMessage());
                }
            } else {
                 model.addAttribute("errorMessage", "Lỗi lưu sản phẩm do ràng buộc dữ liệu không xác định.");
            }

            loadCategories(model); // Load lại categories
            // Giữ lại URL ảnh đã xử lý (có thể là mới hoặc cũ) để hiển thị lại form
            product.setImageUrl(finalImageUrlToSave); // Đảm bảo model có URL đúng
            return "admin/product/form"; // Quay lại form với lỗi

        } catch (Exception e) { // Bắt các lỗi không mong muốn khác khi lưu DB
            log.error("Admin: Unexpected error saving product '{}': {}", product.getName(), e.getMessage(), e); // Log cả stack trace
            model.addAttribute("errorMessage", "Lỗi không mong muốn khi lưu sản phẩm: " + e.getMessage());
=======
        } catch (IOException e) {
            log.error("Admin: Lỗi lưu ảnh: {}", e.getMessage());
            model.addAttribute("errorMessage", "Lỗi lưu ảnh sản phẩm.");
>>>>>>> origin/nnd
            loadCategories(model);
            return "admin/product/form";

        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("slug")) {
                bindingResult.rejectValue("slug", "duplicate.slug", "Slug đã tồn tại.");
            } else if (message.contains("name")) {
                bindingResult.rejectValue("name", "duplicate.name", "Tên sản phẩm đã tồn tại.");
            } else {
                model.addAttribute("errorMessage", "Lỗi ràng buộc dữ liệu.");
            }
            loadCategories(model);
            return "admin/product/form";

        } catch (Exception e) {
            log.error("Admin: Lỗi không mong muốn khi lưu sản phẩm: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Lỗi khi lưu sản phẩm.");
            loadCategories(model);
            return "admin/product/form";
        }
    } 

}