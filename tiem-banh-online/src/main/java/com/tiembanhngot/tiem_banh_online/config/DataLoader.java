package com.tiembanhngot.tiem_banh_online.config;


import com.tiembanhngot.tiem_banh_online.entity.*; // Import các entity cần thiết
import com.tiembanhngot.tiem_banh_online.repository.*; // Import các repository cần thiết
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Thêm thư viện log (optional)
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // Import PasswordEncoder
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.math.BigDecimal;
import java.util.Optional;

@Component // Đánh dấu là một Spring Bean để được Spring quản lý và thực thi
@RequiredArgsConstructor // Sử dụng Lombok để tự tạo constructor cho các field final (Dependency Injection)
@Slf4j // Sử dụng Lombok để tạo logger (optional)
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder để mã hóa mật khẩu

    @Override
    @Transactional // Đảm bảo tất cả các thao tác DB trong hàm này là một giao dịch
    public void run(String... args) throws Exception {
        log.info("Loading initial data...");

        // 1. Tạo Roles nếu chưa tồn tại
        Role adminRole = createRoleIfNotFound("admin");
        Role customerRole = createRoleIfNotFound("customer");

        // 2. Tạo User Admin nếu chưa tồn tại
        createUserIfNotFound("admin@tiembanh.com", "Admin User", "Admin123", "0900000000", adminRole);
        // Bạn có thể tạo thêm user customer mẫu nếu muốn
        // createUserIfNotFound("customer@email.com", "Customer Name", "Cust123", "0911111111", customerRole);

        // 3. Tạo Categories nếu chưa tồn tại
        Category banhKem = createCategoryIfNotFound("Bánh Kem", "Các loại bánh kem sinh nhật, lễ kỷ niệm", "banh-kem");
        Category pastry = createCategoryIfNotFound("Pastry", "Bánh ngọt kiểu Âu", "pastry");
        Category banhMi = createCategoryIfNotFound("Bánh Mì Ngọt", "Các loại bánh mì ăn sáng, ăn nhẹ", "banh-mi-ngot");
        Category cookies = createCategoryIfNotFound("Cookies", "Bánh quy các loại", "cookies");

        // 4. Tạo Products nếu chưa tồn tại (Chỉ tạo nếu các category tương ứng đã được tạo)
        
if (banhKem != null) {
     createProductIfNotFound("Bánh Kem Dâu Tươi", "banh-kem-dau-tuoi",
             "Bánh kem mềm mịn với lớp kem tươi và dâu tây Đà Lạt.",
             new BigDecimal("350000.00"),
             "/img/banhkem_dau.jpg", 
             banhKem);
     createProductIfNotFound("Bánh Kem Chocolate", "banh-kem-chocolate",
             "Cốt bánh chocolate ẩm, phủ ganache chocolate đậm đà.",
             new BigDecimal("380000.00"),
             "/img/banhkem_socola.jpg", 
             banhKem);
}

if (pastry != null) {
    createProductIfNotFound("Croissant Bơ", "croissant-bo",
            "Bánh sừng bò ngàn lớp, thơm lừng mùi bơ Pháp.",
            new BigDecimal("30000.00"),
            "/img/coison_bo.jpg", 
            pastry);
    createProductIfNotFound("Pain au Chocolat", "pain-au-chocolat",
            "Bánh mì cuộn socola đen.",
            new BigDecimal("35000.00"),
            "/img/PainauChocolat.jpg", 
            pastry);
}

if (banhMi != null) {
     // Giả sử bạn có ảnh banh_mi_xuc_xich_pho_mai.jpg trong /img
     createProductIfNotFound("Bánh Mì Xúc Xích Phô Mai", "banh-mi-xuc-xich-phomai",
             "Bánh mì mềm kẹp xúc xích và phô mai tan chảy.",
             new BigDecimal("25000.00"),
             "/img/banh_mi_xuc_xich_pho_mai.jpg", // <<< Sửa lại đường dẫn (thay ảnh mẫu nếu cần)
             banhMi);
}

 if (cookies != null) {
     createProductIfNotFound("Cookies Socola Chip", "cookies-socola-chip",
             "Bánh quy bơ giòn rụm với hạt socola.",
             new BigDecimal("15000.00"),
             "/img/banh_quy_socola.png", // <<< Sửa lại đường dẫn và đuôi file
             cookies);
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
    Category createCategoryIfNotFound(String name, String description, String slug) {
         Optional<Category> catOpt = categoryRepository.findBySlug(slug);
         if (catOpt.isEmpty()) {
             Category newCategory = new Category();
             newCategory.setName(name);
             newCategory.setDescription(description);
             newCategory.setSlug(slug);
             log.info("Creating category: {}", name);
             return categoryRepository.save(newCategory);
         }
         return catOpt.get();
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