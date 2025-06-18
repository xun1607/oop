package com.tiembanhngot.tiem_banh_online.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.CartService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/cart")
@SessionAttributes("shoppingCart")
@Slf4j
public class CartController {
    @Autowired
    private CartService cartService;

    @ModelAttribute("shoppingCart")
    public CartDTO getShoppingCart(HttpSession session) {
        return cartService.getCart(session);
    }

    @GetMapping
    public String viewCart(@ModelAttribute("shoppingCart") CartDTO cart, Model model) {
        model.addAttribute("currentPage", "cart");
        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
            @RequestParam(value = "selectedSize", required = false) String selectedSize, HttpSession session,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(productId, quantity, selectedSize, session);
            redirectAttributes.addFlashAttribute("cartMessageSuccess", "Đã thêm sản phẩm vào giỏ hàng!");

        } catch (ProductNotFoundException | IllegalArgumentException e) {
            log.warn("Error adding product {} to cart: {}", productId, e.getMessage());
            redirectAttributes.addFlashAttribute("cartMessageError", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error adding product {} to cart.", productId, e);
            redirectAttributes.addFlashAttribute("cartMessageError", "Unexpected error when adding to cart.");
        }
        String referer = request.getHeader("Referer");
        log.debug("Redirecting back to referrer: {}", referer);
        return "redirect:" + (referer != null && !referer.contains("/login") && !referer.contains("/register") ? referer
                : "/products");
    }

    @PostMapping("/update")
    public String updateCartItem(@RequestParam("productId") Long productId, @RequestParam("quantity") int quantity,
            String selectedSize,
            HttpSession session, RedirectAttributes redirectAttributes) {
        try {

            cartService.updateQuantity(productId, quantity, selectedSize, session); // <<< CHANGED CALL
            redirectAttributes.addFlashAttribute("cartMessageSuccess", "Product quantity updated.");
        } catch (Exception e) {
            log.error("Error updating cart for product {} (size: {}): {}", productId, selectedSize, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("cartMessageError", "Error updating cart.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String removeCartItem(@PathVariable("productId") Long productId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItemById(productId, session);
            redirectAttributes.addFlashAttribute("cartMessageSuccess", "Product removed from cart.");
        } catch (Exception e) {
            log.error("Error removing product {} from cart.", productId, e);
            redirectAttributes.addFlashAttribute("cartMessageError", "Lỗi khi xóa sản phẩm.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        cartService.clearCart(session);
        redirectAttributes.addFlashAttribute("cartMessageSuccess", "Giỏ hàng đã được xóa thành công.");
        return "redirect:/cart";
    }
}