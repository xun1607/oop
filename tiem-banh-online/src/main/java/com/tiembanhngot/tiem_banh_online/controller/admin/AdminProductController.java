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
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        @PostMapping("/save")
    public String saveProduct(
            // @Valid: Kích hoạt validation cho các annotation trong Product (như @NotBlank, @Size, @NotNull cho price)
            @Valid @ModelAttribute("product") Product product,
            // BindingResult: Chứa kết quả validation, *phải* đặt ngay sau đối tượng được validate (@Valid)
            BindingResult bindingResult,
            // @RequestParam("imageFile"): Lấy file được upload từ input có name="imageFile"
            @RequestParam("imageFile") MultipartFile imageFile,
            // @RequestParam(value = "currentImageUrl", required = false):
            // Lấy giá trị từ input ẩn có name="currentImageUrl" (dùng để lưu URL ảnh cũ khi edit)
            // required = false: Cho phép giá trị này null hoặc rỗng (trường hợp thêm mới)
            @RequestParam(value = "currentImageUrl", required = false) String currentImageUrl,
            @RequestParam Map<String, String> allRequestParams,
            // Model: Để thêm attribute trả về view nếu có lỗi validation hoặc lỗi lưu
            Model model,
            // RedirectAttributes: Để thêm flash attribute (thông báo) khi redirect thành công
            RedirectAttributes redirectAttributes
    ) {

        // Xác định xem đây là thao tác thêm mới hay cập nhật dựa vào productId
        boolean isNew = product.getProductId() == null;
        log.info("Admin: Attempting to save {} product: ID={}, Name={}",
                 isNew ? "new" : "existing",
                 isNew ? "N/A" : product.getProductId(),
                 product.getName() != null ? product.getName() : "<NAME_IS_NULL>"); // Log tên an toàn

        // Chuẩn bị pageTitle để hiển thị trên form (dù thành công hay lỗi)
        String pageTitle = isNew ? "Thêm Sản Phẩm Mới" : "Chỉnh Sửa Sản Phẩm (ID: " + product.getProductId() + ")";
        model.addAttribute("pageTitle", pageTitle);

        // === KIỂM TRA VALIDATION ===

        // 1. Kiểm tra Category thủ công (vì Category là object, @NotNull trên field categoryId không đủ)
         if (product.getCategory() == null || product.getCategory().getCategoryId() == null) {
              // Thêm lỗi vào BindingResult cho trường 'category'
              bindingResult.rejectValue("category", "NotEmpty.product.category", "Vui lòng chọn danh mục.");
              log.warn("Validation Error: Category is not selected.");
         }

        // 2. Kiểm tra các lỗi validation khác từ @Valid (annotation trên Product entity/DTO)
        if (bindingResult.hasErrors()) {
            log.warn("Admin: Validation errors found for product '{}': {}", product.getName(), bindingResult.getAllErrors());
            // Nếu có lỗi, cần load lại danh sách categories cho dropdown trên form
            loadCategories(model);
            // QUAN TRỌNG: Nếu là edit và có lỗi, phải giữ lại ảnh cũ để hiển thị lại trên form
            // Giá trị currentImageUrl được truyền từ view khi edit
            if (!isNew) {
                 product.setImageUrl(currentImageUrl); // Đặt lại URL ảnh cũ vào đối tượng product
                 log.debug("Validation error on update. Restoring current image URL: {}", currentImageUrl);
            }
             // 'product' object (với lỗi và imageUrl cũ nếu có) đã nằm trong model do @ModelAttribute
            return "admin/product/form"; // Trả về lại trang form để hiển thị lỗi
        }
        // --- XỬ LÝ SIZE OPTIONS ---
     Map<String, BigDecimal> sizeOptionsMap = new HashMap<>();
     for (int i = 1; i <= 3; i++) { // Giả sử có tối đa 3 cặp input
         String sizeName = allRequestParams.get("sizeOptions_name_" + i);
         String sizePriceStr = allRequestParams.get("sizeOptions_price_" + i);

         // Chỉ thêm nếu cả tên và giá đều có và giá hợp lệ
         if (StringUtils.hasText(sizeName) && StringUtils.hasText(sizePriceStr)) {
             try { 
                 BigDecimal sizePrice = new BigDecimal(sizePriceStr.replace(",", "")); // Bỏ dấu phẩy nếu có
                 if (sizePrice.compareTo(BigDecimal.ZERO) >= 0) { // Giá phải >= 0
                      sizeOptionsMap.put(sizeName.trim(), sizePrice);
                      log.debug("Added size option: {} - {}", sizeName.trim(), sizePrice);
                 } else {
                      log.warn("Invalid price for size {}: {}", sizeName, sizePriceStr);
                      // Có thể thêm lỗi vào bindingResult nếu muốn
                      // bindingResult.rejectValue("", "InvalidPrice", "Giá size " + i + " không hợp lệ.");
                 }
             } catch (NumberFormatException e) {
                 log.warn("Invalid number format for size price {}: {}", i, sizePriceStr);
                  // bindingResult.rejectValue("", "InvalidPriceFormat", "Định dạng giá size " + i + " không đúng.");
             }
         }
     }
     product.setSizeOptions(sizeOptionsMap); // Gán Map đã tạo vào product
     // --- KẾT THÚC XỬ LÝ SIZE ---
        // === XỬ LÝ UPLOAD ẢNH MỚI (NẾU CÓ) ===

        String newImageUrl = null; // Lưu URL của ảnh MỚI được upload (nếu có)
        String finalImageUrlToSave = null; // URL cuối cùng sẽ được lưu vào DB

        if (imageFile != null && !imageFile.isEmpty()) { // Kiểm tra xem người dùng có chọn file mới không
            log.debug("Admin: New image file provided: '{}', size: {} bytes", imageFile.getOriginalFilename(), imageFile.getSize());
            try {
                // 1. Lưu file vật lý: Gọi StorageService để lưu file, nhận về tên file DUY NHẤT
                String savedFileName = storageService.store(imageFile);
                log.info("Admin: File saved successfully as: {}", savedFileName);

                // 2. Tạo URL để truy cập ảnh: Ghép prefix đã cấu hình trong MvcConfig
                newImageUrl = "/uploads/" + savedFileName;
                finalImageUrlToSave = newImageUrl; // Ảnh mới sẽ được lưu
                log.info("Admin: New image URL generated: {}", newImageUrl);

                // 3. Xóa ảnh cũ (nếu là cập nhật và có ảnh mới khác ảnh cũ)
                 if (!isNew && StringUtils.hasText(currentImageUrl) && !newImageUrl.equals(currentImageUrl)) {
                     log.info("Attempting to delete old image: {}", currentImageUrl);
                     // Chỉ xóa file nếu nó nằm trong thư mục uploads được quản lý
                     if (currentImageUrl.startsWith("/uploads/")) {
                          try {
                              String oldFilename = currentImageUrl.substring("/uploads/".length());
                              storageService.delete(oldFilename); // Gọi service để xóa file vật lý
                              log.info("Successfully deleted old image file: {}", oldFilename);
                          } catch (IOException e) {
                               // Ghi log lỗi xóa ảnh nhưng không dừng tiến trình lưu sản phẩm
                               log.error("Could not delete old image file '{}': {}", currentImageUrl, e.getMessage());
                          }
                      } else {
                           log.warn("Old image URL '{}' does not start with '/uploads/'. Skipping deletion.", currentImageUrl);
                      }
                  }

            } catch (IOException e) {
                // Lỗi xảy ra trong quá trình lưu file ảnh mới
                log.error("Admin: Failed to store uploaded file '{}': {}", imageFile.getOriginalFilename(), e.getMessage());
                // Thêm lỗi vào BindingResult để hiển thị trên form
                // Sử dụng "imageUrl" vì đó là trường logic liên quan, dù lỗi là do file upload
                bindingResult.rejectValue("imageUrl", "upload.error", "Lỗi khi tải ảnh lên: " + e.getMessage());
                loadCategories(model); // Load lại categories
                // Giữ lại ảnh cũ nếu là edit
                if (!isNew) {
                    product.setImageUrl(currentImageUrl);
                }
                return "admin/product/form"; // Quay lại form với lỗi upload
            }
        } else { // Người dùng KHÔNG upload file ảnh mới
            if (isNew) {
                // Thêm mới sản phẩm và không có ảnh -> dùng ảnh mặc định
                finalImageUrlToSave = "/img/placeholder.png"; // Ảnh tĩnh placeholder
                log.debug("No new image uploaded for new product. Setting placeholder image URL.");
            } else {
                // Cập nhật sản phẩm và không có ảnh mới -> giữ nguyên ảnh cũ
                finalImageUrlToSave = currentImageUrl; // Giữ nguyên URL ảnh cũ từ DB
                log.debug("No new image uploaded for existing product. Keeping old image URL: {}", currentImageUrl);
            }
        }

        // === GÁN URL ẢNH CUỐI CÙNG VÀO ĐỐI TƯỢNG PRODUCT ===
        product.setImageUrl(finalImageUrlToSave);
        log.debug("Final imageUrl to be saved for product: {}", finalImageUrlToSave);


        // === GỌI SERVICE ĐỂ LƯU SẢN PHẨM VÀO DATABASE ===
        try {
            log.debug("Calling ProductService.saveProduct...");
            // Truyền đối tượng product đã được cập nhật (bao gồm slug, imageUrl mới/cũ/placeholder)
            productService.saveProduct(product); // Service bây giờ chỉ cần nhận product

            // Thêm thông báo thành công để hiển thị sau khi redirect
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã " + (isNew ? "thêm" : "cập nhật") + " sản phẩm '" + product.getName() + "' thành công!");
            log.info("Product '{}' saved successfully.", product.getName());

            // Chuyển hướng về trang danh sách sản phẩm
            return "redirect:/admin/products";

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
            loadCategories(model);
            product.setImageUrl(finalImageUrlToSave); // Giữ lại URL ảnh đã xử lý
            return "admin/product/form"; // Quay lại form với lỗi
        }
    }

    

}
