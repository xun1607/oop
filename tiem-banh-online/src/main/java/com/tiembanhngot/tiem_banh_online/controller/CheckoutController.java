package com.tiembanhngot.tiem_banh_online.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.OrderDTO;
import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException; // Đảm bảo import đúng
import com.tiembanhngot.tiem_banh_online.service.CartService;
import com.tiembanhngot.tiem_banh_online.service.OrderService;
import com.tiembanhngot.tiem_banh_online.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Controller
@RequestMapping("/checkout")
@SessionAttributes("shoppingCart")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {
    @Autowired
    private  OrderService orderService;
    @Autowired
    private  CartService cartService;
    @Autowired
    private UserService userService;

    @ModelAttribute("shoppingCart")
    public CartDTO getShoppingCart(HttpSession session) {
        CartDTO cart = cartService.getCart(session);
        log.trace("ModelAttribute method: Providing shoppingCart. Session ID: {}. Cart items: {}", session.getId(), cart != null ? cart.getTotalItems() : "null");
        return cart != null ? cart : new CartDTO();
    }


    @GetMapping
    public String showCheckoutPage(Model model, Authentication authentication, HttpSession session) {

        log.info("Accessing checkout page. Session ID: {}", session.getId());

    
        CartDTO cartFromSessionDirectly = (CartDTO) session.getAttribute("shoppingCart");
        CartDTO cartFromModelAttribute = (CartDTO) model.getAttribute("shoppingCart");

        log.info("--- CART DEBUG ---");
        log.info("CART DIRECTLY FROM SESSION (using session.getAttribute): {}", cartFromSessionDirectly);
        if (cartFromSessionDirectly != null) {
            log.info("   -> Direct Session Cart Items: {}, Amount: {}, ItemList is null? {}",
                     cartFromSessionDirectly.getTotalItems(),
                     cartFromSessionDirectly.getTotalAmount(),
                     cartFromSessionDirectly.getItemList() == null); 
            if(cartFromSessionDirectly.getItemList() != null) {
                 log.info("   -> Direct Session Cart ItemList Size: {}", cartFromSessionDirectly.getItemList().size());
            }
        }

        log.info("CART FROM MODEL ATTRIBUTE (using model.getAttribute): {}", cartFromModelAttribute);
        if (cartFromModelAttribute != null) {
             log.info("   -> Model Attribute Cart Items: {}, Amount: {}, ItemList is null? {}",
                      cartFromModelAttribute.getTotalItems(),
                      cartFromModelAttribute.getTotalAmount(),
                      cartFromModelAttribute.getItemList() == null);
             if(cartFromModelAttribute.getItemList() != null) {
                  log.info("   -> Model Attribute Cart ItemList Size: {}", cartFromModelAttribute.getItemList().size());
             }
        }
        log.info("--- END CART DEBUG ---");

        // Ưu tiên sử dụng cart lấy trực tiếp từ session để kiểm tra và xử lý logic chính
        
        CartDTO cart = cartFromSessionDirectly;

        if (cart == null || cart.getItemList() == null || cart.getItemList().isEmpty()) {
            return "redirect:/cart";
        }

        OrderDTO orderDto = new OrderDTO();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
              try {
                 String username = authentication.getName();    //autofill username cho user
                 User currentUser = userService.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found in DB: " + username));
                 if (currentUser.getFullName() != null) orderDto.setRecipientName(currentUser.getFullName());
                 if (currentUser.getPhoneNumber() != null) orderDto.setRecipientPhone(currentUser.getPhoneNumber());
                
            } catch (UsernameNotFoundException e) {
                 log.error("Authenticated user {} not found in DB during checkout.", authentication.getName(), e);
            } catch (Exception e) {
                 log.error("Unexpected error retrieving user info for user {}: {}", authentication.getName(), e.getMessage(), e);
                 model.addAttribute("errorMessage", "Không thể tự động điền thông tin của bạn do lỗi hệ thống.");
            }
        }

        // Đưa dữ liệu VÀO Model để render View
  
        model.addAttribute("cart", cart);
        model.addAttribute("orderDto", orderDto);
        model.addAttribute("currentPage", "checkout");

        log.error("!!! FINAL CHECKOUT CART STATE: totalAmount = {}, totalItems = {}, itemList is null? {}",
                  (cart != null ? cart.getTotalAmount() : "CART_IS_NULL"),
                  (cart != null ? cart.getTotalItems() : "CART_IS_NULL"),
                  (cart != null ? (cart.getItemList() == null) : "CART_IS_NULL"));

        model.addAttribute("cart", cart); 
        return "checkout"; 
    }

 
    @PostMapping("/place-order")
    public String placeOrder(@Valid @ModelAttribute("orderDto") OrderDTO orderDto,
                             BindingResult bindingResult,
                             @ModelAttribute("shoppingCart") CartDTO cart, 
                             HttpSession session,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             SessionStatus sessionStatus) { 

        String currentUserEmail = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : "Guest";

        model.addAttribute("currentPage", "checkout");

        if (bindingResult.hasErrors()) {    // loi dien form -> refresh lai page
            log.warn("Checkout form validation errors detected!");
            bindingResult.getAllErrors().forEach(error -> log.warn("Validation Error: {}", error.toString()));
            return "checkout"; 
        }

        if (cart == null || cart.getItemList() == null || cart.getItemList().isEmpty()) {
            log.error("Cart is null or empty during place order submission. Redirecting to cart page.");
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đã bị trống hoặc có lỗi xảy ra. Vui lòng thử lại.");
            return "redirect:/cart";
        }

        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
             try {
                 currentUser = userService.findByEmail(authentication.getName())
                                          .orElseThrow(() -> new UsernameNotFoundException("Logged in user not found in DB: " + authentication.getName()));
             } catch (UsernameNotFoundException e){
                  model.addAttribute("errorMessage", "Lỗi xác thực tài khoản. Không thể đặt hàng.");
                  return "checkout"; 
             }
        }

        orderDto.setPaymentMethod("COD");

        // Đặt hàng
        try {
            log.info("Calling orderService.placeOrder for user: {}", currentUserEmail);
            Order newOrder = orderService.placeOrder(orderDto, cart, currentUser);
            log.info("Order {} placed successfully with COD by user: {}", newOrder.getOrderCode(), currentUserEmail);

           // xoa gio hang khoi session
            sessionStatus.setComplete();
            // xoa khoi attribute
            session.removeAttribute("shoppingCart"); 
            log.info("Shopping cart cleared for session ID: {}", session.getId());

    
            redirectAttributes.addFlashAttribute("orderSuccessMessage", "Đặt hàng thành công! Mã đơn hàng của bạn là: " + newOrder.getOrderCode());
            redirectAttributes.addFlashAttribute("orderCode", newOrder.getOrderCode()); // Nếu trang success cần mã đơn hàng
            return "redirect:/checkout/success";

        } catch (ProductNotFoundException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Business error during place order: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "checkout"; // Quay lại form checkout với lỗi
        } catch (Exception e) {
            log.error("Unexpected error during place order processing for user {}: {}", currentUserEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống trong quá trình đặt hàng. Vui lòng thử lại hoặc liên hệ hỗ trợ.");
            return "checkout"; 
        }
    }

    @GetMapping("/success")
    public String orderSuccess(Model model) {
        if (!model.containsAttribute("orderSuccessMessage")) { // co message dat hang thanh cong
            log.warn("Direct access to /checkout/success without success message. Redirecting to home.");
            return "redirect:/";
        }
        model.addAttribute("currentPage", "orderSuccess");
        return "order-success";
        }
}