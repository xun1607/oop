package com.tiembanhngot.tiem_banh_online.controller;
import org.springframework.web.bind.support.SessionStatus;
import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.CartService;
import jakarta.servlet.http.HttpServletRequest; // Import HttpServletRequest
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart") // Base path cho tất cả các request trong controller này
@RequiredArgsConstructor
@SessionAttributes("shoppingCart") // Tự động quản lý attribute "shoppingCart" trong session
@Slf4j
public class CartController {

    private final CartService cartService;

    // Lấy hoặc tạo cart và đưa vào model trước mỗi request tới controller này
    @ModelAttribute("shoppingCart")
    public CartDTO getShoppingCart(HttpSession session) {
        return cartService.getCart(session);
    }

    // Hiển thị trang giỏ hàng
    @GetMapping
    public String viewCart(@ModelAttribute("shoppingCart") CartDTO cart, Model model) {
        model.addAttribute("currentPage", "cart"); // Cho active link header
        model.addAttribute("cart", cart); // Đảm bảo cart luôn có trong model cho view
        log.debug("Displaying cart view with {} items.", cart.getTotalItems());
        return "cart"; // --> /templates/cart.html
    }

    // === BỔ SUNG CÁC PHƯƠNG THỨC POST ===

    // Xử lý thêm sản phẩm vào giỏ
    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                           // @ModelAttribute("shoppingCart") CartDTO cart, // Không cần inject cart ở đây nữa vì service sẽ lấy từ session
                           @RequestParam(value = "selectedSize", required = false) String selectedSize,
                            HttpSession session, // Truyền session vào service
                            HttpServletRequest request, // Để lấy referer URL
                            RedirectAttributes redirectAttributes) {
        log.info("Received request to add product ID: {} with quantity: {}, selectedSize: {}", productId, quantity, selectedSize);
        try {
            cartService.addToCart(productId, quantity, selectedSize, session);
        redirectAttributes.addFlashAttribute("cartMessageSuccess", "Đã thêm sản phẩm vào giỏ hàng!");
            log.info("Successfully added product ID: {} to cart.", productId);
        } catch (ProductNotFoundException | IllegalArgumentException e) {
            log.warn("Error adding product {} to cart: {}", productId, e.getMessage());
            redirectAttributes.addFlashAttribute("cartMessageError", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding product {} to cart.", productId, e);
            redirectAttributes.addFlashAttribute("cartMessageError", "Lỗi không mong muốn khi thêm vào giỏ hàng.");
        }

        // Quay lại trang trước đó
        String referer = request.getHeader("Referer");
        log.debug("Redirecting back to referrer: {}", referer);
        // Tránh redirect về login nếu trang trước đó là login (gây vòng lặp)
        return "redirect:" + (referer != null && !referer.contains("/login") && !referer.contains("/register") ? referer : "/products");
    }

    // Xử lý cập nhật số lượng sản phẩm
    @PostMapping("/update")
    public String updateCartItem(@RequestParam("productId") Long productId,
                                 @RequestParam("quantity") int quantity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
         log.info("Received request to update product ID: {} quantity to: {}.", productId, quantity);
         try {
             cartService.updateQuantity(productId, quantity, session);
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Đã cập nhật số lượng sản phẩm.");
         } catch (Exception e) {
             log.error("Error updating cart for product {}", productId, e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Lỗi khi cập nhật giỏ hàng.");
         }
        return "redirect:/cart"; // Luôn redirect về trang giỏ hàng sau khi cập nhật
    }

    // Xử lý xóa sản phẩm khỏi giỏ hàng
    @PostMapping("/remove/{productId}") // Dùng PathVariable để lấy ID từ URL
    public String removeCartItem(@PathVariable("productId") Long productId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
         log.info("Received request to remove product ID: {} from cart.", productId);
         try {
             cartService.removeItem(productId, session);
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Đã xóa sản phẩm khỏi giỏ hàng.");
         } catch (Exception e) {
              log.error("Error removing product {} from cart.", productId, e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Lỗi khi xóa sản phẩm.");
         }
        return "redirect:/cart"; // Luôn redirect về trang giỏ hàng sau khi xóa
    }

    // Xử lý xóa toàn bộ giỏ hàng
    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes, SessionStatus status) { // Thêm SessionStatus
        log.info("Received request to clear cart using SessionStatus.");
        try {
             // Thay vì gọi cartService.clearCart(session);
             status.setComplete(); // Đánh dấu session attribute được quản lý bởi @SessionAttributes là hoàn thành -> sẽ bị xóa khỏi session khi redirect
             //session.removeAttribute(CART_SESSION_KEY); // Vẫn nên giữ lại để xóa trực tiếp phòng trường hợp @SessionAttributes không hoạt động như mong đợi
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Giỏ hàng đã được xóa.");
             log.info("Cart marked as complete via SessionStatus and removed attribute directly.");
        } catch (Exception e) {
             log.error("Error clearing cart.", e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Lỗi khi xóa giỏ hàng.");
        }
        return "redirect:/cart";
    }
}