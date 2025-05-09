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
import org.springframework.transaction.annotation.Transactional; // Optional, nếu có thao tác DB phức tạp hơn
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.Map; // Sử dụng Map
import java.util.LinkedHashMap; // Để giữ thứ tự


@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private static final String CART_SESSION_KEY = "shoppingCart"; // Key để lưu cart trong session
    private final ProductRepository productRepository;

    /**
     * Lấy giỏ hàng từ HttpSession.
     * Nếu chưa có, tạo mới một giỏ hàng rỗng (với totalAmount = 0) và lưu vào session.
     * Đảm bảo luôn trả về một đối tượng CartDTO hợp lệ, không bao giờ null.
     *
     * @param session HttpSession hiện tại.
     * @return CartDTO từ session hoặc một CartDTO mới nếu chưa tồn tại.
     */
    public CartDTO getCart(HttpSession session) {
        // Cẩn thận: Luôn kiểm tra kiểu dữ liệu khi lấy từ session
        Object cartObject = session.getAttribute(CART_SESSION_KEY);
        CartDTO cart = null;

        if (cartObject instanceof CartDTO) {
            cart = (CartDTO) cartObject;
            log.trace("Cart found in session [ID: {}]. Items: {}", session.getId(), cart.getTotalItems());
             // Đảm bảo các thành phần cốt lõi không null nếu cart đã tồn tại
            if (cart.getItems() == null) {
                log.warn("Cart found in session but items map is null. Initializing.");
                cart.setItems(new LinkedHashMap<>());
            }
            if (cart.getTotalAmount() == null) {
                 log.warn("Cart found in session but totalAmount is null. Initializing to ZERO.");
                 cart.setTotalAmount(BigDecimal.ZERO);
            }
        }

        if (cart == null) {
            log.info("No cart found in session [ID: {}]. Creating a new cart.", session.getId());
            cart = new CartDTO();
            // Khởi tạo các giá trị mặc định quan trọng để tránh NullPointerException sau này
            cart.setItems(new LinkedHashMap<>()); // Dùng LinkedHashMap để giữ thứ tự thêm vào
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setTotalItems(0);
            session.setAttribute(CART_SESSION_KEY, cart);
            log.debug("New empty cart created and stored in session [ID: {}].", session.getId());
        }
        return cart;
    }

    /**
     * Thêm sản phẩm vào giỏ hàng hoặc cập nhật số lượng nếu đã tồn tại.
     * Tự động tính toán lại tổng tiền sau khi thêm/cập nhật.
     *
     * @param productId ID của sản phẩm cần thêm.
     * @param quantity Số lượng cần thêm (phải lớn hơn 0).
     * @param session HttpSession hiện tại.
     * @throws ProductNotFoundException Nếu không tìm thấy sản phẩm với ID cung cấp.
     * @throws IllegalArgumentException Nếu sản phẩm không có sẵn hoặc số lượng không hợp lệ.
     */
    @Transactional(readOnly = true) // Chỉ đọc product, không thay đổi DB ở đây
    public void addToCart(Long productId, int quantity, String selectedSize, HttpSession session) {
        if (quantity <= 0) {
            log.warn("Attempted to add non-positive quantity ({}) for product ID: {}. Ignoring.", quantity, productId);
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0."); // Ném lỗi để controller xử lý
        }

        CartDTO cart = getCart(session); // Lấy cart (sẽ tạo mới nếu chưa có)

        // Lấy thông tin sản phẩm từ DB
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        // Kiểm tra sản phẩm có sẵn không
        if (!Boolean.TRUE.equals(product.getIsAvailable())) { // Cách kiểm tra Boolean an toàn với null
             log.warn("Attempted to add unavailable product ID: {} ('{}') to cart.", productId, product.getName());
             throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' hiện không có sẵn.");
        }
        // Kiểm tra giá có null không (quan trọng)
         if (product.getPrice() == null) {
              log.error("Product ID: {} ('{}') has a NULL price. Cannot add to cart.", productId, product.getName());
              throw new IllegalStateException("Sản phẩm '" + product.getName() + "' đang gặp lỗi về giá. Vui lòng liên hệ hỗ trợ.");
         }
        // --- XÁC ĐỊNH GIÁ DỰA TRÊN SIZE ---
    BigDecimal priceToUse = product.getPrice(); // Mặc định dùng giá gốc
    String sizeIdentifier = selectedSize; // Size để lưu vào DTO

    // Kiểm tra xem sản phẩm có tùy chọn size không và người dùng có chọn size không
    if (StringUtils.hasText(selectedSize) && product.getSizeOptions() != null && !product.getSizeOptions().isEmpty()) {
        if (product.getSizeOptions().containsKey(selectedSize)) {
            // Lấy giá từ map size
            priceToUse = product.getSizeOptions().get(selectedSize);
            log.debug("Using price for selected size '{}': {}", selectedSize, priceToUse);
            // Kiểm tra giá của size có null không
             if (priceToUse == null) {
                  log.error("Price for selected size '{}' of product ID {} is NULL.", selectedSize, productId);
                  throw new IllegalStateException("Sản phẩm '" + product.getName() + "' đang gặp lỗi về giá cho size đã chọn.");
             }
        } else {
            // Người dùng chọn size không hợp lệ -> Dùng giá mặc định và cảnh báo
            log.warn("Invalid size '{}' selected for product ID {}. Using default price {}.", selectedSize, productId, product.getPrice());
            sizeIdentifier = null; // Không lưu size không hợp lệ
            priceToUse = product.getPrice(); // Đảm bảo dùng giá mặc định
             if (priceToUse == null) { // Kiểm tra lại giá mặc định
                  log.error("Default price for product ID {} is NULL.", productId);
                  throw new IllegalStateException("Sản phẩm '" + product.getName() + "' đang gặp lỗi về giá.");
             }
        }
    } else {
         // Không chọn size hoặc sản phẩm không có size -> dùng giá mặc định
         log.debug("No valid size selected or product has no size options. Using default price {}.", product.getPrice());
         sizeIdentifier = null;
         priceToUse = product.getPrice();
         if (priceToUse == null) {
              log.error("Default price for product ID {} is NULL.", productId);
              throw new IllegalStateException("Sản phẩm '" + product.getName() + "' đang gặp lỗi về giá.");
         }
    }
    // --- KẾT THÚC XÁC ĐỊNH GIÁ ---

        


    String itemKey = productId + (sizeIdentifier != null ? "_" + sizeIdentifier : ""); // Tạo key String
CartItemDTO existingItem = cart.getItems().get(itemKey);
        if (existingItem != null) {
            // Sản phẩm đã có, cập nhật số lượng
            log.debug("Updating quantity for existing product ID {} in cart [Session: {}]. Old quantity: {}, Add quantity: {}",
                      productId, session.getId(), existingItem.getQuantity(), quantity);
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // Sản phẩm mới, thêm vào giỏ
            log.debug("Adding new product ID {} to cart [Session: {}] with quantity: {}", productId, session.getId(), quantity);
            CartItemDTO newItem = new CartItemDTO();
        newItem.setProductId(product.getProductId());
        newItem.setName(product.getName());
        newItem.setImageUrl(product.getImageUrl());
        newItem.setPrice(priceToUse); // <<< SỬ DỤNG GIÁ ĐÃ XÁC ĐỊNH
        newItem.setQuantity(quantity);
        newItem.setSlug(product.getSlug());
        newItem.setSelectedSize(sizeIdentifier); // <<< LƯU SIZE ĐÃ CHỌN
        cart.getItems().put(itemKey, newItem); // Key có thể cần bao gồm cả size
        }

        // Tính toán lại tổng tiền và số lượng
        updateCartTotals(cart);

        log.info("Cart updated after adding/updating product ID {}. Session ID: {}. Total items: {}, Total amount: {}",
                 productId, session.getId(), cart.getTotalItems(), cart.getTotalAmount());

        // Lưu ý: Không cần gọi session.setAttribute() lần nữa nếu bạn chỉ sửa đổi
        // các thuộc tính của đối tượng `cart` đã lấy từ session.
        // Tuy nhiên, nếu bạn tạo đối tượng cart mới hoàn toàn trong logic nào đó (ít gặp),
        // bạn sẽ cần gọi session.setAttribute().
    }

    /**
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng.
     * Nếu số lượng <= 0, sản phẩm sẽ bị xóa khỏi giỏ.
     *
     * @param productId ID của sản phẩm cần cập nhật.
     * @param quantity Số lượng mới (nếu <= 0 sẽ xóa).
     * @param session HttpSession hiện tại.
     */
    public void updateQuantity(Long productId, int quantity, HttpSession session) {
        CartDTO cart = getCart(session);
        CartItemDTO item = cart.getItems().get(productId);

        if (item != null) {
            if (quantity > 0) {
                log.debug("Updating quantity for product ID {} in cart [Session: {}] to {}.", productId, session.getId(), quantity);
                item.setQuantity(quantity);
            } else {
                // Số lượng <= 0, xóa sản phẩm
                log.debug("Quantity for product ID {} is <= 0. Removing item from cart [Session: {}].", productId, session.getId());
                cart.getItems().remove(productId);
            }
            // Tính toán lại tổng tiền và số lượng
            updateCartTotals(cart);
            log.info("Cart updated after quantity change for product ID {}. Session ID: {}. Total items: {}, Total amount: {}",
                      productId, session.getId(), cart.getTotalItems(), cart.getTotalAmount());
        } else {
             log.warn("Attempted to update quantity for non-existent product ID {} in cart [Session: {}].", productId, session.getId());
             // Có thể ném exception nếu muốn báo lỗi rõ ràng hơn
             // throw new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ hàng để cập nhật.");
        }
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng.
     *
     * @param productId ID của sản phẩm cần xóa.
     * @param session HttpSession hiện tại.
     */
    public void removeItem(Long productId, HttpSession session) {
        CartDTO cart = getCart(session);
        log.debug("Attempting to remove product ID {} from cart [Session: {}].", productId, session.getId());

        CartItemDTO removedItem = cart.getItems().remove(productId);

        if (removedItem != null) {
            log.debug("Successfully removed product ID {} from cart.", productId);
            // Tính toán lại tổng tiền và số lượng
            updateCartTotals(cart);
             log.info("Cart updated after removing product ID {}. Session ID: {}. Total items: {}, Total amount: {}",
                      productId, session.getId(), cart.getTotalItems(), cart.getTotalAmount());
        } else {
            log.warn("Attempted to remove non-existent product ID {} from cart [Session: {}].", productId, session.getId());
             // Có thể ném exception nếu muốn
             // throw new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ hàng để xóa.");
        }
    }

    /**
     * Xóa tất cả sản phẩm khỏi giỏ hàng bằng cách xóa attribute khỏi session.
     *
     * @param session HttpSession hiện tại.
     */
    public void clearCart(HttpSession session) {
        log.info("Attempting to clear cart for session ID: {}", session.getId());
        if (session.getAttribute(CART_SESSION_KEY) != null) {
            session.removeAttribute(CART_SESSION_KEY);
            log.info("Cart attribute '{}' removed successfully for session ID: {}.", CART_SESSION_KEY, session.getId());
            // Kiểm tra lại để chắc chắn
            if (session.getAttribute(CART_SESSION_KEY) == null) {
                 log.debug("Confirmed: Cart attribute '{}' is null after removal.", CART_SESSION_KEY);
            } else {
                 log.error("CRITICAL: Cart attribute '{}' STILL EXISTS after attempting removal for session ID: {}!", CART_SESSION_KEY, session.getId());
            }
        } else {
            log.warn("No cart attribute '{}' found in session ID {} to clear.", CART_SESSION_KEY, session.getId());
        }
    }

    /**
     * Lấy tổng số lượng các mặt hàng (tính cả số lượng của từng mặt hàng) trong giỏ.
     *
     * @param session HttpSession hiện tại.
     * @return Tổng số lượng items.
     */
     public int getCartItemCount(HttpSession session) {
         // Lấy cart và trả về totalItems đã được tính toán
         return getCart(session).getTotalItems();
     }

    /**
     * Lấy tổng giá trị tiền của giỏ hàng.
     *
     * @param session HttpSession hiện tại.
     * @return Tổng tiền (BigDecimal).
     */
    public BigDecimal getCartTotal(HttpSession session) {
        // Lấy cart và trả về totalAmount đã được tính toán
        // Đảm bảo getCart luôn trả về cart hợp lệ với totalAmount không null
        return getCart(session).getTotalAmount();
    }


    /**
     * Tính toán lại tổng tiền (totalAmount) và tổng số lượng item (totalItems) trong giỏ hàng.
     * Cập nhật trực tiếp các thuộc tính của đối tượng CartDTO được truyền vào.
     * Đảm bảo totalAmount luôn là BigDecimal.ZERO nếu không có item hoặc có lỗi.
     *
     * @param cart Đối tượng CartDTO cần được tính toán lại.
     */
    private void updateCartTotals(CartDTO cart) {
        if (cart == null) {
            log.error("Cannot update totals for a null cart.");
            return;
        }
        log.debug("Recalculating totals for cart...");
        // ***** LUÔN KHỞI TẠO BẰNG ZERO *****
        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;
        int calculatedTotalItems = 0;

        if (cart.getItems() != null) {
            for (CartItemDTO item : cart.getItems().values()) {
                if (item != null && item.getPrice() != null && item.getQuantity() > 0) {
                    try {
                        BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                        item.setLineTotal(lineTotal);
                        calculatedTotalAmount = calculatedTotalAmount.add(lineTotal);
                        calculatedTotalItems += item.getQuantity();
                    } catch (ArithmeticException e) {
                         log.error("ArithmeticException for item productId: {}", item.getProductId(), e);
                         item.setLineTotal(BigDecimal.ZERO);
                    }
                } else {
                    log.warn("Skipping item total calculation due to invalid data: {}", item);
                    if (item != null) item.setLineTotal(BigDecimal.ZERO);
                }
            }
        } else {
            log.warn("Cart items map is null. Totals will be zero.");
        }
        // ***** LUÔN GÁN GIÁ TRỊ VÀO CART *****
        cart.setTotalAmount(calculatedTotalAmount);
        cart.setTotalItems(calculatedTotalItems);
        log.info("Cart totals recalculated: Items={}, Amount={}", cart.getTotalItems(), cart.getTotalAmount());
    }
}