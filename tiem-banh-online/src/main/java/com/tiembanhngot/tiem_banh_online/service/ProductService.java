package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException; // Đảm bảo import đúng
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer; // Thêm import này cho generateSlug
import java.util.List;
import java.util.Locale;    // Thêm import này cho generateSlug
import java.util.Optional;
import java.util.regex.Pattern; // Thêm import này cho generateSlug

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]"); // Cho generateSlug
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]"); // Cho generateSlug

    // --- Các phương thức tìm kiếm ---
    @Transactional(readOnly = true)
    public List<Product> findAllAvailableProducts() {
        return productRepository.findByIsAvailableTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) { // **PHƯƠNG THỨC NÀY CẦN CÓ**
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Product> findAllProductsPaginated(Pageable pageable) { // **PHƯƠNG THỨC NÀY CẦN CÓ**
        return productRepository.findAll(pageable);
    }

    // --- Các phương thức CRUD cho Admin ---
    @Transactional
    public Product saveProduct(Product product) { // **PHƯƠNG THỨC NÀY CẦN CÓ**
        if (!StringUtils.hasText(product.getSlug()) || (product.getProductId() == null && productRepository.findBySlug(product.getSlug()).isPresent())) {
             String baseSlug = generateSlug(product.getName());
             String finalSlug = baseSlug;
             int counter = 1;
             // Kiểm tra trùng lặp và tạo slug duy nhất
             while (productRepository.findBySlug(finalSlug).filter(p -> !p.getProductId().equals(product.getProductId())).isPresent()) {
                 finalSlug = baseSlug + "-" + counter++;
             }
             product.setSlug(finalSlug);
         } else if (product.getProductId() != null) {
             // Nếu là cập nhật, kiểm tra xem slug có bị trùng với sản phẩm khác không
              Optional<Product> existingBySlug = productRepository.findBySlug(product.getSlug());
              if (existingBySlug.isPresent() && !existingBySlug.get().getProductId().equals(product.getProductId())) {
                  throw new IllegalArgumentException("Slug '" + product.getSlug() + "' đã được sử dụng bởi sản phẩm khác.");
              }
         }
        // Bổ sung: Đảm bảo category được gắn đúng cách nếu cần thiết
        // if (product.getCategory() != null && product.getCategory().getCategoryId() == null) {
        //     throw new IllegalArgumentException("Category không hợp lệ.");
        // }
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProductById(Long id) { // **PHƯƠNG THỨC NÀY CẦN CÓ**
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + id + " để xóa.");
        }
        // Cần kiểm tra xem có đơn hàng nào liên quan không trước khi xóa
        // if (orderItemRepository.existsByProductProductId(id)) { // Giả sử có method này
        //     throw new DataIntegrityViolationException("Không thể xóa sản phẩm vì đã có trong đơn hàng.");
        // }
        productRepository.deleteById(id);
    }

    // Hàm tạo slug an toàn hơn
    public static String generateSlug(String input) {
        if (input == null) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
        return slug.isEmpty() ? "san-pham" : slug; // Trả về slug mặc định nếu rỗng
    }
}