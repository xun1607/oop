package com.tiembanhngot.tiem_banh_online.controller.admin; // Tạo package admin

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.service.CategoryService;
import com.tiembanhngot.tiem_banh_online.service.ProductService;
// import com.tiembanhngot.tiem_banh_online.service.StorageService; // Sẽ cần cho lưu file ảnh
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới truy cập được các endpoint trong controller này
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    // private final StorageService storageService; // Inject service xử lý lưu file

    @GetMapping
    public String listProducts(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "productId,asc") String sort, // ví dụ: name,desc
                               Model model) {
        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Product> productPage = productService.findAllProductsPaginated(pageable); // Cần thêm method này vào ProductService

        model.addAttribute("productPage", productPage);
        model.addAttribute("sort", sort); // để giữ lại thông tin sort cho view
        // Các thông tin khác cho phân trang nếu cần (totalPages, number...)
        return "admin/product/list"; // -> /templates/admin/product/list.html
    }

    private void addCategoriesToModel(Model model) {
        List<Category> categories = categoryService.findAllCategories();
        model.addAttribute("categories", categories);
    }

    @GetMapping("/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product()); // Product rỗng cho form binding
        addCategoriesToModel(model); // Thêm danh sách category vào model
        model.addAttribute("pageTitle", "Thêm Sản Phẩm Mới");
        return "admin/product/form"; // -> /templates/admin/product/form.html
    }

    @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + id));
            model.addAttribute("product", product);
            addCategoriesToModel(model);
            model.addAttribute("pageTitle", "Chỉnh Sửa Sản Phẩm (ID: " + id + ")");
            return "admin/product/form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product, // Nhận Product trực tiếp hoặc tạo ProductDTO
                              BindingResult bindingResult,
                              @RequestParam("imageFile") MultipartFile imageFile, // Nhận file ảnh upload
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
             addCategoriesToModel(model); // Cần load lại category khi có lỗi validation
             model.addAttribute("pageTitle", product.getProductId() == null ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")");
            return "admin/product/form"; // Trả về form nếu có lỗi
        }

        // ----- Xử lý Upload File Ảnh -----
        if (!imageFile.isEmpty()) {
            try {
                // String savedFileName = storageService.store(imageFile); // Gọi service lưu file
                // product.setImageUrl("/uploads/" + savedFileName); // Cập nhật đường dẫn ảnh mới (Ví dụ)
                // --- TẠM THỜI CHƯA IMPLEMENT LƯU FILE ---
                 product.setImageUrl("/img/placeholder.png"); // Hoặc giữ ảnh cũ nếu là edit và không up ảnh mới
                 System.out.println("Received file: " + imageFile.getOriginalFilename()); // Log tạm
            } catch (Exception e) {
                bindingResult.rejectValue("imageUrl", "upload.error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                addCategoriesToModel(model);
                model.addAttribute("pageTitle", product.getProductId() == null ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")");
                return "admin/product/form";
            }
        } else if (product.getProductId() != null) {
             // Nếu là edit và không upload ảnh mới, giữ lại ảnh cũ
            productService.findById(product.getProductId()).ifPresent(existingProduct -> {
                product.setImageUrl(existingProduct.getImageUrl());
            });
        }
        // ----- Kết thúc xử lý Upload -----

        try {
            productService.saveProduct(product); // Cần thêm/sửa method này trong ProductService
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu sản phẩm thành công!");
            return "redirect:/admin/products";
        } catch (Exception e) {
             // Log lỗi e
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi lưu sản phẩm.");
             addCategoriesToModel(model);
             model.addAttribute("pageTitle", product.getProductId() == null ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")");
            return "admin/product/form"; // Quay lại form với thông báo lỗi
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProductById(id); // Cần thêm method này vào ProductService
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm ID: " + id);
        } catch (Exception e) {
             // Log lỗi e
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa sản phẩm ID: " + id + ". " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
}