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
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.repository.CategoryRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    Product createProductIfNotFound(String name, String description,
                                    BigDecimal defaultPrice, String imageUrl, Category category,
                                    Map<String, BigDecimal> sizeOptions) {
        Optional<Product> prodOpt = productRepository.findByName(name); // Tìm theo tên
        if (prodOpt.isEmpty()) {
            Product newProduct = new Product();
            newProduct.setName(name);
            newProduct.setDescription(description);
            newProduct.setPrice(defaultPrice);
            newProduct.setImageUrl(imageUrl);
            newProduct.setCategory(category);
            newProduct.setIsAvailable(true);
            newProduct.setSizeOptions(sizeOptions != null ? new HashMap<>(sizeOptions) : new HashMap<>());

            log.info("Creating product (DataLoader): Name='{}', Image='{}', Sizes='{}'",
                     name, imageUrl, newProduct.getSizeOptions());
            return productRepository.save(newProduct);
        } else {
            log.info("Product '{}' already exists. Skipping creation.", name);
            return prodOpt.get();
        }
    }

    @Transactional
    Category createCategoryIfNotFound(String name, String description) {
        Optional<Category> catOpt = categoryRepository.findByName(name);
        if (catOpt.isEmpty()) {
            Category newCategory = new Category();
            newCategory.setName(name);
            newCategory.setDescription(description);
            log.info("Creating category: {}", name);
            Category saved = categoryRepository.save(newCategory);
            categoryRepository.flush();
            return saved;
        }
        return catOpt.get();
    }

    @Transactional
    User createUserIfNotFound(String email, String fullName, String rawPassword, String phone, User.Role role) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
            newUser.setPhoneNumber(phone);
            newUser.setRole(role);
            log.info("Creating user: {}", email);
            return userRepository.save(newUser);
        }
        return userOpt.get();
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Loading initial data...");

        Category cake = createCategoryIfNotFound("Cake", "Birthday and celebration cakes.");
        Category bread = createCategoryIfNotFound("Bread", "Various types of European-style bread and croissants.");
        Category brunch = createCategoryIfNotFound("Brunch", "Light meals and brunch items.");
        Category dessert = createCategoryIfNotFound("Dessert", "Mini cakes and sweet treats.");
        Category macaroon = createCategoryIfNotFound("Macaroon", "Specialty French-style macaroons.");

        if (bread != null) {
            createProductIfNotFound("Coconut Charcoal Croissant", "Coconut, Milk, Charcoal", new BigDecimal("38000"), "/img/coconut-charcoal-croissant.png", bread, null);
            createProductIfNotFound("Corn Sausage Croissant", "Sausage, Corn, Cheese, Egg", new BigDecimal("42000"), "/img/corn-sausage-croissant.png", bread, null);
            createProductIfNotFound("Cream Cheese Brioche", "Creamcheese, Egg, Milk", new BigDecimal("36000"), "/img/cream-cheese-brioche.png", bread, null);
            createProductIfNotFound("Cream Soboro", "Chocolate, Peanut, Fresh cream, Egg", new BigDecimal("40000"), "/img/cream-soboro.png", bread, null);
            createProductIfNotFound("Baguette", "Flour, Levain, Water", new BigDecimal("28000"), "/img/baguette.png", bread, null);
            createProductIfNotFound("Crookie", "Butter, Egg, Chocolate", new BigDecimal("35000"), "/img/crookie.png", bread, null);
            createProductIfNotFound("Dark Chocolate Donut", "Dark chocolate, Milk chocolate, Egg", new BigDecimal("30000"), "/img/dark-chocolate-donut.png", bread, null);
            createProductIfNotFound("Egg Tart Portugal", "Egg, Fresh cream, Vanilla", new BigDecimal("28000"), "/img/egg-tart-portugal.png", bread, null);
        }

        if (cake != null) {
            createProductIfNotFound("Beloved Darling", "Blueberry, Fresh cream, Yogurt", new BigDecimal("265000"), "/img/beloved-darling.png", cake, null);
            createProductIfNotFound("A Little Grace", "Carrot, Creamcheese, Walnut, Cinnamon", new BigDecimal("260000"), "/img/a-little-grace.png", cake, null);
            createProductIfNotFound("Little Hopper", "Fresh cream, Dark chocolate, Cacao powder", new BigDecimal("300000"), "/img/little-hopper.png", cake, null);
            createProductIfNotFound("Red Velvet", "Vanilla bean, Milk, Mascarpone", new BigDecimal("200000"), "/img/red-velvet.png", cake, null);
            createProductIfNotFound("One Sunny Day", "Strawberry, Fresh cream, Milk", new BigDecimal("350000"), "/img/one-sunny-day.png", cake, null);
            createProductIfNotFound("The Blessing", "Strawberry, Vanilla, Milk", new BigDecimal("500000"), "/img/the-blessing.png", cake, null);
        }

        if (brunch != null) {
            createProductIfNotFound("Cajun Chicken Salad", "Cajun chicken salad", new BigDecimal("150000"), "/img/cajun-chicken-salad.png", brunch, null);
            createProductIfNotFound("Cranberry Chicken Sandwich", "Chicken breast, Cranberry, Walnut", new BigDecimal("90000"), "/img/cranberry-chicken-sandwich.png", brunch, null);
            createProductIfNotFound("Egg Mayo Sandwich", "Egg, Mayonnaise, Strawberry", new BigDecimal("95000"), "/img/egg-mayo-sandwich.png", brunch, null);
            createProductIfNotFound("Ham Cheese Danish", "Tomato, Ham, Cheese", new BigDecimal("100000"), "/img/ham-cheese-danish.png", brunch, null);
            createProductIfNotFound("Sweet Chili Cold Pasta", "Chicken, Cheese, Spaghetti", new BigDecimal("72000"), "/img/sweet-chili-cold-pasta.png", brunch, null);
        }

        if (dessert != null) {
            createProductIfNotFound("Basque Cheesecake (Piece)", "Fresh cream, Creamcheese, Vanilla", new BigDecimal("60000"), "/img/basque-cheesecake-piece.png", dessert, null);
            createProductIfNotFound("Black&Pink Brownie", "White chocolate, Strawberry, Dark chocolate", new BigDecimal("65000"), "/img/black-pink-brownie.png", dessert, null);
            createProductIfNotFound("Carrot Cake (Piece)", "Carrot, Creamcheese, Cinnamon, Walnut", new BigDecimal("55000"), "/img/carrot-cake-piece.png", dessert, null);
            createProductIfNotFound("Cream Cheese Roll (Piece)", "Creamcheese, Fresh cream, Vanilla bean", new BigDecimal("50000"), "/img/cream-cheese-roll-piece.png", dessert, null);
            createProductIfNotFound("Dark Chocolate Roll (Piece)", "Dark chocolate, Fresh cream, Cacao powder", new BigDecimal("56000"), "/img/dark-chocolate-roll-piece.png", dessert, null);
        }

        if (macaroon != null) {
            createProductIfNotFound("Big Macaroon Cheese Pretzel", "Smoke cheese, Pretzel, Fresh cream", new BigDecimal("45000"), "/img/big-macaroon-cheese-pretzel.png", macaroon, null);
            createProductIfNotFound("Big Macaroon Chocolat", "Cacao nibs, Dark chocolat, Almond", new BigDecimal("47000"), "/img/big-macaroon-chocolat.png", macaroon, null);
            createProductIfNotFound("Big Macaroon Coconut", "Coconut, Coconut milk, Almond", new BigDecimal("46000"), "/img/big-macaroon-coconut.png", macaroon, null);
            createProductIfNotFound("Big Macaroon Lemon", "Lemon, White chocolat, Fresh cream", new BigDecimal("44000"), "/img/big-macaroon-lemon.png", macaroon, null);
            createProductIfNotFound("Big Macaroon Matcha", "Matcha, Dark chocolat, Almond", new BigDecimal("48000"), "/img/big-macaroon-matcha.png", macaroon, null);
        }
        createUserIfNotFound(
            "admin@tiembanhngon.vn",
            "Quản trị viên",
            "admin123",
            "0000000000",
            User.Role.ADMIN
        );

        log.info("Finished loading product data.");
    }
}
