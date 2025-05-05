package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.CategoryService;
import com.tiembanhngot.tiem_banh_online.service.ProductService;
import com.tiembanhngot.tiem_banh_online.service.StorageService; // Import StorageService
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final StorageService storageService; // **Inject StorageService**

    // Hàm tiện ích load Categories
    private void loadCategories(Model model) {
        List<Category> categories = categoryService.findAllCategories();
        model.addAttribute("categories", categories);
    }

    // Hiển thị danh sách sản phẩm
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "productId,asc") String sort,
            Model model) {

        log.info("Admin: Request received for product list: page={}, size={}, sort={}", page, size, sort);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.ASC;
        String sortField = sortParams[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Product> productPage = productService.findAllProductsPaginated(pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);

        model.addAttribute("currentPage", "products"); // <-- THÊM DÒNG NÀY
        log.debug("Admin: Returning product list view...");
        return "admin/product/list";
    }

    // Hiển thị form thêm
    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        log.debug("Admin: Request received for add product form.");
        model.addAttribute("product", new Product());
        loadCategories(model);
        model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
        return "admin/product/form";
    }

    // Hiển thị form sửa
     @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Admin: Request received to edit product with ID: {}", id);
        try {
            Product product = productService.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
            model.addAttribute("product", product);
            loadCategories(model);
            model.addAttribute("pageTitle", "Chỉnh Sửa Sản Phẩm (ID: " + id + ")");
            log.debug("Admin: Displaying edit form for product ID: {}", id);
            return "admin/product/form";
        } catch (ProductNotFoundException e) {
            log.warn("Admin: Product not found for editing: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // Xử lý lưu sản phẩm
    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult bindingResult,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              // Lấy ảnh cũ từ hidden input nếu là edit
                              @RequestParam(value = "currentImageUrl", required = false) String currentImageUrl,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        boolean isNew = product.getProductId() == null;
        log.info("Admin: Attempting to save {} product: {}", isNew ? "new" : "existing", product.getName());
        String pageTitle = isNew ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")";
        model.addAttribute("pageTitle", pageTitle); // Set lại title phòng khi lỗi

        // Kiểm tra Category có được chọn không
         if (product.getCategory() == null || product.getCategory().getCategoryId() == null) {
              bindingResult.rejectValue("category", "NotEmpty.product.category", "Vui lòng chọn danh mục.");
         }

        if (bindingResult.hasErrors()) {
            log.warn("Admin: Validation errors found for product {}: {}", product.getName(), bindingResult.getAllErrors());
            loadCategories(model);
            // Giữ lại ảnh cũ nếu là edit và có lỗi
            if (!isNew) product.setImageUrl(currentImageUrl);
            return "admin/product/form";
        }

        // Xử lý Upload Ảnh Mới (nếu có)
        String newImageUrl = null; // Reset URL mới
        if (!imageFile.isEmpty()) {
            try {
                log.debug("Admin: Processing uploaded file: {}", imageFile.getOriginalFilename());
                String savedFileName = storageService.store(imageFile);
                newImageUrl = "/uploads/" + savedFileName; // Đường dẫn URL sau khi lưu
                product.setImageUrl(newImageUrl); // Gán URL mới cho product object
                 log.info("Admin: New image saved as: {}, setting imageUrl.", savedFileName);
            } catch (IOException e) {
                log.error("Admin: Failed to store uploaded file for product {}: {}", product.getName(), e.getMessage());
                // Thêm lỗi vào bindingResult để hiển thị trên form
                bindingResult.rejectValue("imageUrl", "upload.error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                loadCategories(model);
                 if (!isNew) product.setImageUrl(currentImageUrl); // Giữ lại ảnh cũ
                return "admin/product/form";
            }
        } else {
             product.setImageUrl(null); // Đặt là null nếu không có ảnh mới (Service sẽ xử lý giữ ảnh cũ)
        }


        // Lưu sản phẩm vào DB (truyền cả ảnh cũ)
        try {
            log.debug("Admin: Calling ProductService.saveProduct");
            productService.saveProduct(product, isNew ? null : currentImageUrl); // Truyền oldImageUrl nếu là update
            redirectAttributes.addFlashAttribute("successMessage", "Đã " + (isNew ? "thêm" : "cập nhật") + " sản phẩm thành công!");
            log.info("Admin: Product {} successfully.", isNew ? "created" : "updated");
            return "redirect:/admin/products";
        } catch (DataIntegrityViolationException e) {
            log.error("Admin: Data integrity violation for product {}: {}", product.getName(), e.getMessage());
            bindingResult.rejectValue("slug", "duplicate", e.getMessage()); // Thêm lỗi vào field slug (hoặc name)
            loadCategories(model);
            product.setImageUrl(newImageUrl != null ? newImageUrl : currentImageUrl); // Giữ lại URL ảnh đã xử lý
            return "admin/product/form";
        } catch (Exception e) {
            log.error("Admin: Error saving product {}: {}", product.getName(), e);
            model.addAttribute("errorMessage", "Lỗi khi lưu sản phẩm: " + e.getMessage()); // Thêm lỗi chung
            loadCategories(model);
             product.setImageUrl(newImageUrl != null ? newImageUrl : currentImageUrl); // Giữ lại URL ảnh đã xử lý
            return "admin/product/form";
        }
    }

    // Xử lý xóa sản phẩm
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        log.info("Admin: Request received to delete product with ID: {}", id);
        try {
            productService.deleteProductById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm ID: " + id);
            log.info("Admin: Successfully deleted product ID: {}", id);
        } catch (ProductNotFoundException e) {
             log.warn("Admin: Attempted to delete non-existent product: {}", e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
         catch (DataIntegrityViolationException e) { // Bắt lỗi ràng buộc (ví dụ: đơn hàng)
            log.error("Admin: Cannot delete product ID: {} due to existing references.", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage()); // Hiển thị lỗi từ service
        } catch (Exception e) {
            log.error("Admin: Error deleting product ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không mong muốn khi xóa sản phẩm ID: " + id);
        }
        return "redirect:/admin/products";
    }
}