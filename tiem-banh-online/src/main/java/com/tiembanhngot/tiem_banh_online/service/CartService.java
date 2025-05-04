package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.CartItemDTO;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// Import Map thay vì List
import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final String CART_SESSION_KEY = "shoppingCart";
    private final ProductRepository productRepository;

    /**
     * Lấy giỏ hàng từ HttpSession. Nếu chưa có, tạo mới và lưu vào session.
     */
    public CartDTO getCart(HttpSession session) {
        CartDTO cart = (CartDTO) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            log.debug("Creating new cart for session ID: {}", session.getId());
            cart = new CartDTO();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng hoặc cập nhật số lượng nếu đã tồn tại.
     */
    public void addToCart(Long productId, int quantity, HttpSession session) {
        if (quantity <= 0) {
            log.warn("Attempted to add non-positive quantity ({}) for product ID: {}", quantity, productId);
            return;
        }

        CartDTO cart = getCart(session);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Cannot add to cart. Product not found with ID: " + productId));

        if (!product.getIsAvailable()) {
             log.warn("Attempted to add unavailable product ID: {}", productId);
             throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' hiện không có sẵn.");
        }

        CartItemDTO existingItem = cart.getItems().get(productId);

        if (existingItem != null) {
            // Sản phẩm đã có, cập nhật số lượng
            log.debug("Updating quantity for product ID {} in cart. Old quantity: {}, Add quantity: {}", productId, existingItem.getQuantity(), quantity);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // Sản phẩm mới, thêm vào giỏ
            log.debug("Adding new product ID {} to cart with quantity: {}", productId, quantity);
            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(product.getProductId());
            newItem.setName(product.getName());
            newItem.setImageUrl(product.getImageUrl());
            newItem.setPrice(product.getPrice()); // Lấy giá hiện tại
            newItem.setQuantity(quantity);
            newItem.setSlug(product.getSlug()); // Lưu slug
            cart.getItems().put(productId, newItem);
        }

        updateCartTotals(cart);
        log.info("Cart updated for session ID: {}. Total items: {}, Total amount: {}", session.getId(), cart.getTotalItems(), cart.getTotalAmount());
        // Session attribute tự động cập nhật vì ta sửa đổi object cart lấy từ session
        // session.setAttribute(CART_SESSION_KEY, cart); // Không cần thiết nếu object cart được sửa trực tiếp
    }

    /**
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng.
     * Nếu số lượng <= 0, sản phẩm sẽ bị xóa.
     */
    public void updateQuantity(Long productId, int quantity, HttpSession session) {
        CartDTO cart = getCart(session);
        CartItemDTO item = cart.getItems().get(productId);

        if (item != null) {
            if (quantity > 0) {
                log.debug("Updating quantity for product ID {} to {}.", productId, quantity);
                item.setQuantity(quantity);
            } else {
                // Số lượng <= 0, xóa sản phẩm
                log.debug("Removing product ID {} due to zero/negative quantity update.", productId);
                cart.getItems().remove(productId);
            }
            updateCartTotals(cart);
             log.info("Cart updated (quantity change) for session ID: {}. Total items: {}, Total amount: {}", session.getId(), cart.getTotalItems(), cart.getTotalAmount());
        } else {
             log.warn("Attempted to update quantity for non-existent product ID {} in cart.", productId);
        }
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng.
     */
    public void removeItem(Long productId, HttpSession session) {
        CartDTO cart = getCart(session);
        CartItemDTO removedItem = cart.getItems().remove(productId);

        if (removedItem != null) {
            log.debug("Removed product ID {} from cart.", productId);
            updateCartTotals(cart);
             log.info("Cart updated (item removed) for session ID: {}. Total items: {}, Total amount: {}", session.getId(), cart.getTotalItems(), cart.getTotalAmount());
        } else {
            log.warn("Attempted to remove non-existent product ID {} from cart.", productId);
        }
    }

    /**
     * Lấy tổng số lượng các sản phẩm trong giỏ.
     */
     public int getCartItemCount(HttpSession session) {
         return getCart(session).getTotalItems();
     }

    /**
     * Lấy tổng giá trị tiền của giỏ hàng.
     */
    public BigDecimal getCartTotal(HttpSession session) {
        return getCart(session).getTotalAmount();
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng.
     */
    public void clearCart(HttpSession session) {
        log.info("Clearing cart for session ID: {}", session.getId());
        session.removeAttribute(CART_SESSION_KEY);
    }

    /**
     * Tính toán lại tổng tiền và tổng số lượng item trong giỏ hàng.
     */
    private void updateCartTotals(CartDTO cart) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;
        for (CartItemDTO item : cart.getItems().values()) { // Lặp qua values của Map
            // Tính thành tiền cho từng dòng
            item.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            // Cộng dồn tổng tiền
            totalAmount = totalAmount.add(item.getLineTotal());
            // Cộng dồn tổng số lượng
            totalItems += item.getQuantity();
        }
        cart.setTotalAmount(totalAmount);
        cart.setTotalItems(totalItems);
        log.trace("Recalculated cart totals: Items={}, Amount={}", totalItems, totalAmount);
    }
}