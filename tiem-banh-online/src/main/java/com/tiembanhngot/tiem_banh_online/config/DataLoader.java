package com.tiembanhngot.tiem_banh_online.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tiembanhngot.tiem_banh_online.entity.Category;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.entity.Role;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.repository.CategoryRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import com.tiembanhngot.tiem_banh_online.repository.RoleRepository;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    // === CHỈ GIỮ LẠI PHIÊN BẢN PHƯƠNG THỨC NÀY (7 THAM SỐ) ===
    @Transactional // Đặt Transactional ở đây cũng được
    Product createProductIfNotFound(String name, String slug, String description,
                                    BigDecimal defaultPrice, String imageUrl, Category category,
                                    Map<String, BigDecimal> sizeOptions) { // Thêm tham số sizeOptions
        Optional<Product> prodOpt = productRepository.findBySlug(slug);
        if (prodOpt.isEmpty()) {
            Product newProduct = new Product();
            newProduct.setName(name);
            // TODO: Nên gọi hàm tạo slug chuẩn ở đây nếu slug chưa được chuẩn hóa
            // newProduct.setSlug(ProductService.generateSlug(slug)); // Ví dụ
            newProduct.setSlug(slug); // Tạm thời giữ nguyên slug truyền vào
            newProduct.setDescription(description);
            newProduct.setPrice(defaultPrice); // Giá mặc định/cơ bản
            newProduct.setImageUrl(imageUrl); // URL ảnh mẫu (vd: /img/...)
            newProduct.setCategory(category);
            newProduct.setIsAvailable(true);

            // Set size options nếu được cung cấp và không rỗng
            if (sizeOptions != null && !sizeOptions.isEmpty()) {
                newProduct.setSizeOptions(new HashMap<>(sizeOptions)); // Tạo bản sao để an toàn
            } else {
                 newProduct.setSizeOptions(new HashMap<>()); // Luôn khởi tạo Map rỗng
            }

            log.info("Creating product (DataLoader): Name='{}', Slug='{}', Image='{}', Sizes='{}'",
                     name, newProduct.getSlug(), imageUrl, newProduct.getSizeOptions());
            return productRepository.save(newProduct);
        } else {
             log.info("Product with slug '{}' already exists. Skipping creation.", slug);
             return prodOpt.get();
        }
    }
    // === KẾT THÚC PHIÊN BẢN PHƯƠNG THỨC MỚI ===


    @Override
    @Transactional // Đảm bảo tất cả các thao tác DB trong hàm này là một giao dịch
    public void run(String... args) throws Exception {
        log.info("Loading initial data...");

        // 1. Tạo Roles
        Role adminRole = createRoleIfNotFound("ADMIN"); // Chuẩn hóa tên Role
        Role customerRole = createRoleIfNotFound("CUSTOMER");

        // 2. Tạo User Admin
        createUserIfNotFound("admin@tiembanh.com", "Admin User", "Admin123", "0900000000", adminRole);
        // createUserIfNotFound("customer@email.com", "Customer Name", "Cust123", "0911111111", customerRole);

        // 3. Tạo Categories
        Category banhKem = createCategoryIfNotFound("Bánh Kem", "Các loại bánh kem sinh nhật, lễ kỷ niệm", "banh-kem");
        Category pastry = createCategoryIfNotFound("Pastry", "Bánh ngọt kiểu Âu", "pastry");
        Category banhMi = createCategoryIfNotFound("Bánh Mì Ngọt", "Các loại bánh mì ăn sáng, ăn nhẹ", "banh-mi-ngot");
        Category cookies = createCategoryIfNotFound("Cookies", "Bánh quy các loại", "cookies");

        // --- 4. Tạo Products ---
        // Đảm bảo đường dẫn ảnh trỏ đến file có thật trong /static/img/...
        // Đảm bảo truyền tham số thứ 7 (sizeOptions) là null hoặc Map

        if (banhKem != null) {
            // Sản phẩm có size
            Map<String, BigDecimal> dauSizes = new HashMap<>();
            dauSizes.put("Nhỏ (18cm)", new BigDecimal("350000.00"));
            dauSizes.put("Vừa (22cm)", new BigDecimal("450000.00"));
            dauSizes.put("Lớn (25cm)", new BigDecimal("550000.00"));
            createProductIfNotFound(
                "Bánh Kem Dâu Tươi", "banh-kem-dau-tuoi",
                "Bánh kem mềm mịn với lớp kem tươi và dâu tây Đà Lạt.",
                new BigDecimal("350000.00"), // Giá mặc định (size nhỏ nhất)
                "/img/products/banhkem_dau.jpg", // <<< Đảm bảo ảnh này tồn tại
                banhKem,
                dauSizes // <<< Truyền Map size vào
            );

            // Sản phẩm không có size
            createProductIfNotFound(
                "Bánh Kem Chocolate", "banh-kem-chocolate",
                "Cốt bánh chocolate ẩm, phủ ganache chocolate đậm đà.",
                new BigDecimal("380000.00"),
                "/img/products/banhkem_socola.jpg", // <<< Đảm bảo ảnh này tồn tại
                banhKem,
                null // <<< Truyền null cho sizeOptions
            );
        }

        if (pastry != null) {
             // Sản phẩm không có size
            createProductIfNotFound(
                "Croissant Bơ", "croissant-bo",
                "Bánh sừng bò ngàn lớp, thơm lừng mùi bơ Pháp.",
                new BigDecimal("30000.00"),
                "/img/products/croissant.jpg", // <<< Đảm bảo ảnh này tồn tại
                pastry,
                null // <<< Truyền null
            );
             // Sản phẩm không có size
            createProductIfNotFound(
                "Pain au Chocolat", "pain-au-chocolat",
                "Bánh mì cuộn socola đen.",
                new BigDecimal("35000.00"),
                "/img/products/PainauChocolat.jpg", // <<< Đảm bảo ảnh này tồn tại
                pastry,
                null // <<< Truyền null
            );
        }

        if (banhMi != null) {
             // Sản phẩm không có size
             createProductIfNotFound(
                 "Bánh Mì Xúc Xích Phô Mai", "banh-mi-xuc-xich-phomai",
                 "Bánh mì mềm kẹp xúc xích và phô mai tan chảy.",
                 new BigDecimal("25000.00"),
                 "/img/products/banh_mi_xuc_xich_pho_mai.jpg", // <<< Đảm bảo ảnh này tồn tại
                 banhMi,
                 null // <<< Truyền null
             );
        }

        if (cookies != null) {
             // Sản phẩm không có size
             createProductIfNotFound(
                 "Cookies Socola Chip", "cookies-socola-chip",
                 "Bánh quy bơ giòn rụm với hạt socola.",
                 new BigDecimal("15000.00"),
                 "/img/products/banh_quy_socola.png", // <<< Đảm bảo ảnh này tồn tại
                 cookies,
                 null // <<< Truyền null
             );
        }

        log.info("Finished loading initial data.");
    }


    // --- Helper Methods ---

    @Transactional
    Role createRoleIfNotFound(String roleName) {
        Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
        if (roleOpt.isEmpty()) {
            Role newRole = new Role();
            newRole.setRoleName(roleName);
            log.info("Creating role: {}", roleName);
            return roleRepository.save(newRole);
        }
        return roleOpt.get();
    }

    @Transactional
    User createUserIfNotFound(String email, String fullName, String rawPassword, String phone, Role role) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setPasswordHash(passwordEncoder.encode(rawPassword)); // Mã hóa mật khẩu
            newUser.setPhoneNumber(phone);
            newUser.setRole(role);
            log.info("Creating user: {}", email);
            return userRepository.save(newUser);
        }
        return userOpt.get();
    }

    

     @Transactional
     Product createProductIfNotFound(String name, String slug, String description, BigDecimal price, String imageUrl, Category category) {
         Optional<Product> prodOpt = productRepository.findBySlug(slug);
         if (prodOpt.isEmpty()) {
             Product newProduct = new Product();
             newProduct.setName(name);
             newProduct.setSlug(slug);
             newProduct.setDescription(description);
             newProduct.setPrice(price);
             newProduct.setImageUrl(imageUrl); // Đảm bảo ảnh có tồn tại trong static/img hoặc là URL thật
             newProduct.setCategory(category);
             newProduct.setIsAvailable(true);
             log.info("Creating product: {}", name);
             return productRepository.save(newProduct);
         }
         return prodOpt.get();
     }
}