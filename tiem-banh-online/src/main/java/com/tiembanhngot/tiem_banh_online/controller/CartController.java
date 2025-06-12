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

    @ModelAttribute("shoppingCart")
    public CartDTO getShoppingCart(HttpSession session) {
        return cartService.getCart(session);
    }

    @GetMapping
    public String viewCart(@ModelAttribute("shoppingCart") CartDTO cart, Model model) {
        model.addAttribute("currentPage", "cart");
        model.addAttribute("cart", cart);
        log.debug("Displaying cart view with {} items.", cart.getTotalItems());
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            @RequestParam(value = "selectedSize", required = false) String selectedSize,
                            HttpSession session,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        log.info("Received request to add product ID: {} with quantity: {}, selectedSize: {}", productId, quantity, selectedSize);
        try {
            cartService.addToCart(productId, quantity, selectedSize, session);
            redirectAttributes.addFlashAttribute("cartMessageSuccess", "Product added to cart!");
            log.info("Successfully added product ID: {} to cart.", productId);
        } catch (ProductNotFoundException | IllegalArgumentException e) {
            log.warn("Error adding product {} to cart: {}", productId, e.getMessage());
            redirectAttributes.addFlashAttribute("cartMessageError", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding product {} to cart.", productId, e);
            redirectAttributes.addFlashAttribute("cartMessageError", "Unexpected error when adding to cart.");
        }
        String referer = request.getHeader("Referer");
        log.debug("Redirecting back to referrer: {}", referer);
        return "redirect:" + (referer != null && !referer.contains("/login") && !referer.contains("/register") ? referer : "/products");
    }

    // Handle product quantity update
    @PostMapping("/update")
    public String updateCartItem(@RequestParam("productId") Long productId,
                                 @RequestParam("quantity") int quantity,
                                 @RequestParam(value = "selectedSize", required = false) String selectedSize, // <<< NEW PARAMETER
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
         log.info("Received request to update product ID: {} with selectedSize: {} to quantity: {}.", productId, selectedSize, quantity);
         try {
             // Pass selectedSize to the service method
             cartService.updateQuantity(productId, quantity, selectedSize, session); // <<< CHANGED CALL
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Product quantity updated.");
         } catch (Exception e) {
             log.error("Error updating cart for product {} (size: {}): {}", productId, selectedSize, e.getMessage(), e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Error updating cart.");
         }
        return "redirect:/cart";
    }

    // Handle product removal from cart
    // For consistency, removeItem should also be modified similarly if you want to remove a specific size variant
    @PostMapping("/remove") // Should change from @PathVariable if you use selectedSize
    public String removeCartItem(@RequestParam("productId") Long productId,
                                 @RequestParam(value = "selectedSize", required = false) String selectedSize, // <<< NEW PARAMETER
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
         log.info("Received request to remove product ID: {} with selectedSize: {} from cart.", productId, selectedSize);
         try {
             cartService.removeItem(productId, selectedSize, session); // <<< CHANGED CALL
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Product removed from cart.");
         } catch (Exception e) {
              log.error("Error removing product {} (size: {}) from cart.", productId, selectedSize, e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Error removing product.");
         }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes, SessionStatus status) {
        log.info("Received request to clear cart using SessionStatus.");
        try {
             status.setComplete();
             redirectAttributes.addFlashAttribute("cartMessageSuccess", "Cart has been cleared.");
             log.info("Cart marked as complete via SessionStatus and attribute removed directly.");
        } catch (Exception e) {
             log.error("Error clearing cart.", e);
             redirectAttributes.addFlashAttribute("cartMessageError", "Error clearing cart.");
        }
        return "redirect:/cart";
    }
}