
package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.entity.Product;
// Import exception nếu bạn đã tạo file riêng
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.OrderRepository; // Import nếu cần kiểm tra order trước khi xóa
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException; // Import để bắt lỗi constraint
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException; // Import cho lỗi IO khi xóa ảnh cũ
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j // Bật logging
public class ProductService {

    private final ProductRepository productRepository;
    private final StorageService storageService; // Inject service xử lý file ảnh
    // private final OrderItemRepository orderItemRepository; // Inject nếu cần kiểm tra order item

    // Patterns for slug generation
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // --- Phương thức cho trang người dùng ---

    /**
     * Lấy danh sách sản phẩm đang bán, sắp xếp mới nhất lên đầu.
     */
    @Transactional(readOnly = true)
    public List<Product> findAllAvailableProducts() {
        log.debug("Fetching all available products, ordered by creation date desc.");
        return productRepository.findByIsAvailableTrueOrderByCreatedAtDesc();
    }

    /**
     * Tìm sản phẩm theo slug (dùng cho trang chi tiết sản phẩm).
     */
    @Transactional(readOnly = true)
    public Optional<Product> findBySlug(String slug) {
        log.debug("Finding product by slug: {}", slug);
        return productRepository.findBySlug(slug);
    }

    // --- Phương thức cho trang Admin ---

    /**
     * Tìm sản phẩm theo ID (dùng cho trang sửa admin).
     */
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        log.debug("Finding product by ID: {}", id);
        return productRepository.findById(id);
    }

    /**
     * Lấy tất cả sản phẩm có phân trang và sắp xếp (dùng cho trang list admin).
     */
    @Transactional(readOnly = true)
    public Page<Product> findAllProductsPaginated(Pageable pageable) {
        log.debug("Fetching all products with pagination: {}", pageable);
        return productRepository.findAll(pageable);
    }

    /**
     * Lưu sản phẩm (Thêm mới hoặc Cập nhật).
     * Xử lý slug tự động, kiểm tra trùng lặp slug.
     * Xử lý URL ảnh dựa trên việc có upload file mới hay không.
     * Xóa ảnh cũ nếu có ảnh mới được upload khi cập nhật.
     *
     * @param product     Đối tượng Product chứa thông tin từ form. `imageUrl` trong đối tượng này
     *                    sẽ là URL mới nếu có file upload, hoặc null/rỗng nếu không.
     * @param oldImageUrl URL ảnh cũ của sản phẩm (chỉ có giá trị khi cập nhật). Dùng để xóa file cũ.
     * @return Product đã được lưu.
     * @throws DataIntegrityViolationException Nếu slug bị trùng.
     * @throws IllegalArgumentException Nếu tên sản phẩm rỗng khi tạo slug.
     */
    @Transactional
    public Product saveProduct(Product product, String oldImageUrl) {
        boolean isNew = product.getProductId() == null;
        log.info("Attempting to save {} product: ID={}, Name={}",
                 isNew ? "new" : "existing",
                 isNew ? "N/A" : product.getProductId(),
                 product.getName());

        // 1. Xử lý và kiểm tra Slug
        if (!StringUtils.hasText(product.getName())) {
             throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }

        String targetSlug = product.getSlug();
        if (!StringUtils.hasText(targetSlug)) { // Nếu slug rỗng -> tạo tự động
            targetSlug = generateSlug(product.getName());
            log.debug("Generated initial slug: {}", targetSlug);
        }

        String finalSlug = ensureUniqueSlug(targetSlug, product.getProductId());
        product.setSlug(finalSlug);
        log.debug("Final slug set to: {}", finalSlug);


        // 2. Xử lý Image URL và xóa ảnh cũ (nếu cần)
        String newImageUrl = product.getImageUrl(); // URL mới được set từ controller nếu có upload

        if (isNew && !StringUtils.hasText(newImageUrl)) {
            // Nếu thêm mới và không upload ảnh, đặt ảnh mặc định
            product.setImageUrl("/img/placeholder.png"); // Hoặc null tùy yêu cầu
            log.debug("Setting placeholder image for new product.");
        } else if (!isNew && !StringUtils.hasText(newImageUrl)) {
            // Nếu cập nhật và không upload ảnh mới, giữ nguyên ảnh cũ
            product.setImageUrl(oldImageUrl);
            log.debug("Keeping old image URL: {}", oldImageUrl);
        } else if (StringUtils.hasText(newImageUrl) && !newImageUrl.equals(oldImageUrl) && StringUtils.hasText(oldImageUrl)) {
            // Nếu có ảnh mới, khác ảnh cũ, và ảnh cũ tồn tại -> Xóa ảnh cũ
            log.info("New image provided [{}], attempting to delete old image [{}]", newImageUrl, oldImageUrl);
            try {
                // Chỉ xóa nếu ảnh cũ nằm trong thư mục upload của mình
                if (oldImageUrl.startsWith("/uploads/")) {
                    String oldFilename = oldImageUrl.substring("/uploads/".length());
                    storageService.delete(oldFilename);
                    log.info("Successfully deleted old image file: {}", oldFilename);
                } else {
                     log.warn("Old image URL [{}] does not seem to be in the managed upload directory. Skipping deletion.", oldImageUrl);
                }
            } catch (IOException e) {
                // Ghi log lỗi xóa ảnh cũ nhưng không nên dừng việc lưu sản phẩm
                log.error("Could not delete old image file: {}", oldImageUrl, e);
            }
        }
         // Nếu isNew và có newImageUrl, hoặc update và có newImageUrl -> imageUrl đã được controller set đúng

        // 3. Lưu sản phẩm vào DB
        try {
            Product savedProduct = productRepository.save(product);
            log.info("Product {} successfully with ID: {}", isNew ? "created" : "updated", savedProduct.getProductId());
            return savedProduct;
        } catch (DataIntegrityViolationException e) {
            // Xử lý lỗi constraint (ví dụ: unique slug nếu kiểm tra ở trên bị race condition)
            log.error("Data integrity violation while saving product {}: {}", product.getName(), e.getMessage());
            // Cố gắng xác định lỗi cụ thể dựa vào message (phụ thuộc DB)
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("slug")) {
                 throw new DataIntegrityViolationException("Slug '" + product.getSlug() + "' đã tồn tại. Vui lòng thử tên khác.", e);
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("name")) { // Ví dụ nếu có constraint tên
                 throw new DataIntegrityViolationException("Tên sản phẩm '" + product.getName() + "' đã tồn tại.", e);
            }
            throw new RuntimeException("Lỗi khi lưu sản phẩm: " + e.getMessage(), e);
        }
    }


    /**
     * Đảm bảo slug là duy nhất trong database.
     * Nếu slug đã tồn tại cho sản phẩm khác, sẽ thêm hậu tố "-1", "-2",...
     * @param slug Slug ban đầu (từ tên hoặc từ input)
     * @param currentProductId ID của sản phẩm hiện tại (null nếu là sản phẩm mới)
     * @return Slug duy nhất cuối cùng
     */
    private String ensureUniqueSlug(String slug, Long currentProductId) {
        String finalSlug = slug;
        int counter = 1;
        Optional<Product> existingProduct;
        do {
            existingProduct = productRepository.findBySlug(finalSlug);
            // Slug hợp lệ nếu nó không tồn tại, HOẶC nó tồn tại nhưng thuộc về chính sản phẩm đang sửa
            if (existingProduct.isPresent() && (currentProductId == null || !existingProduct.get().getProductId().equals(currentProductId))) {
                // Slug đã tồn tại cho sản phẩm khác, tạo slug mới
                finalSlug = slug + "-" + counter++;
                log.trace("Slug conflict detected. Trying next slug: {}", finalSlug);
            } else {
                // Slug hợp lệ hoặc chưa tồn tại
                break;
            }
        } while (true);
        return finalSlug;
    }


    /**
     * Xóa sản phẩm theo ID.
     * @param id ID của sản phẩm cần xóa.
     * @throws ProductNotFoundException Nếu không tìm thấy sản phẩm.
     * @throws DataIntegrityViolationException Nếu sản phẩm đang được tham chiếu (ví dụ: trong đơn hàng).
     */
    @Transactional
    public void deleteProductById(Long id) {
        log.info("Attempting to delete product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id + " để xóa."));

        // **QUAN TRỌNG:** Kiểm tra ràng buộc trước khi xóa
        // Ví dụ: Kiểm tra xem sản phẩm có trong OrderItem nào không
        // if (orderItemRepository.existsByProductProductId(id)) { // Cần method này trong OrderItemRepository
        //     log.warn("Attempted to delete product ID: {} which exists in orders.", id);
        //     throw new DataIntegrityViolationException("Không thể xóa sản phẩm ID: " + id + " vì đã tồn tại trong đơn hàng.");
        // }

        String imageUrlToDelete = product.getImageUrl(); // Lấy URL ảnh để xóa file

        try {
            productRepository.delete(product); // Xóa sản phẩm khỏi DB
            log.info("Successfully deleted product entity ID: {}", id);

            // Xóa file ảnh liên quan (nếu có và nằm trong thư mục quản lý)
             if (StringUtils.hasText(imageUrlToDelete) && imageUrlToDelete.startsWith("/uploads/")) {
                 try {
                     String filenameToDelete = imageUrlToDelete.substring("/uploads/".length());
                     storageService.delete(filenameToDelete);
                     log.info("Successfully deleted associated image file: {}", filenameToDelete);
                 } catch (IOException e) {
                      log.error("Could not delete image file {} for deleted product ID {}", imageUrlToDelete, id, e);
                      // Không cần ném lỗi ở đây, vì sản phẩm DB đã xóa
                 }
             }

        } catch (DataIntegrityViolationException e) { // Bắt lỗi nếu DB constraint vẫn ngăn xóa
            log.error("Cannot delete product ID: {} due to data integrity violation.", id, e);
             throw new DataIntegrityViolationException("Không thể xóa sản phẩm ID: " + id + " do ràng buộc dữ liệu (có thể do đơn hàng).", e);
        }
    }

    /**
     * Hàm tạo slug từ tên sản phẩm (cải thiện).
     * Chuyển thành chữ thường, bỏ dấu, thay khoảng trắng bằng '-', xóa ký tự đặc biệt.
     */
    public static String generateSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "san-pham-" + System.currentTimeMillis(); // Slug ngẫu nhiên nếu tên rỗng
        }
        // Bỏ dấu tiếng Việt
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // Thay thế ký tự đ
        normalized = normalized.replaceAll("[đĐ]", "d");
        // Xóa các ký tự dấu thanh
        String temp = NONLATIN.matcher(normalized).replaceAll("");
        // Thay khoảng trắng bằng gạch nối
        String slug = WHITESPACE.matcher(temp).replaceAll("-");
        // Chuyển thành chữ thường và xóa các ký tự không hợp lệ còn lại
        slug = slug.toLowerCase(Locale.ENGLISH)
                   .replaceAll("[^a-z0-9\\-]", "") // Chỉ giữ chữ cái, số, gạch nối
                   .replaceAll("-{2,}", "-") // Thay nhiều gạch nối thành 1
                   .replaceAll("^-|-$", ""); // Xóa gạch nối đầu/cuối

        // Trả về slug mặc định nếu kết quả là rỗng
        return slug.isEmpty() ? "san-pham-" + System.currentTimeMillis() : slug;
    }
}
