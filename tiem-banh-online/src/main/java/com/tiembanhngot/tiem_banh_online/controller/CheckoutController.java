package com.tiembanhngot.tiem_banh_online.controller;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus; // Import SessionStatus nếu dùng cách xóa cart khác
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/checkout")
@SessionAttributes("shoppingCart") // Vẫn giữ lại để Spring quản lý session attribute này
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

 // Phương thức @ModelAttribute vẫn giữ nguyên để Spring lấy/tạo cart ban đầu
    @ModelAttribute("shoppingCart")
    public CartDTO getShoppingCart(HttpSession session) {
        CartDTO cart = cartService.getCart(session);
        log.trace("ModelAttribute method: Providing shoppingCart. Session ID: {}. Cart items: {}", session.getId(), cart != null ? cart.getTotalItems() : "null");
        // Đảm bảo luôn trả về một đối tượng hợp lệ, không null
        return cart != null ? cart : new CartDTO();
    }


    @GetMapping
    public String showCheckoutPage(Model model, // Inject Model
                                   Authentication authentication,
                                   HttpSession session) { // Inject HttpSession

        log.info("Accessing checkout page. Session ID: {}", session.getId());

        // --- DEBUGGING BLOCK ---
        // 1. Lấy cart TRỰC TIẾP từ HttpSession
        CartDTO cartFromSessionDirectly = (CartDTO) session.getAttribute("shoppingCart");

        // 2. Lấy cart từ Model (do @ModelAttribute("shoppingCart") đã đặt vào trước đó)
        CartDTO cartFromModelAttribute = (CartDTO) model.getAttribute("shoppingCart");

        log.info("--- CART DEBUG ---");
        log.info("CART DIRECTLY FROM SESSION (using session.getAttribute): {}", cartFromSessionDirectly);
        if (cartFromSessionDirectly != null) {
            log.info("   -> Direct Session Cart Items: {}, Amount: {}, ItemList is null? {}",
                     cartFromSessionDirectly.getTotalItems(),
                     cartFromSessionDirectly.getTotalAmount(),
                     cartFromSessionDirectly.getItemList() == null); // Kiểm tra cả itemList
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
        // --- END DEBUGGING BLOCK ---

        // Ưu tiên sử dụng cart lấy trực tiếp từ session để kiểm tra và xử lý logic chính
        // vì nó phản ánh trạng thái thực tế của session attribute tại thời điểm này.
        CartDTO cart = cartFromSessionDirectly;

        // 1. Kiểm tra giỏ hàng (sử dụng cart lấy trực tiếp từ session)
        if (cart == null || cart.getItemList() == null || cart.getItemList().isEmpty()) {
            log.warn("Cart retrieved directly from session is null or effectively empty. Redirecting to cart.");
            // Không cần thêm cart vào model vì sẽ redirect
            return "redirect:/cart";
        }

        OrderDTO orderDto = new OrderDTO();

        // 2. Điền sẵn thông tin nếu user đăng nhập (Logic này giữ nguyên)
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
             // ... (khối try-catch lấy thông tin user như cũ) ...
              try {
                 String username = authentication.getName();
                 log.debug("Attempting to pre-fill checkout form for user: {}", username);
                 User currentUser = userService.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found in DB: " + username));
                 log.debug("User found: {}. Attempting to set recipient info.", currentUser.getUserId());
                 if (currentUser.getFullName() != null) orderDto.setRecipientName(currentUser.getFullName());
                 if (currentUser.getPhoneNumber() != null) orderDto.setRecipientPhone(currentUser.getPhoneNumber());
                 log.debug("Recipient info pre-filled for user: {}", username);
            } catch (UsernameNotFoundException e) {
                 log.error("Authenticated user {} not found in DB during checkout.", authentication.getName(), e);
            } catch (Exception e) {
                 log.error("Unexpected error retrieving user info for user {}: {}", authentication.getName(), e.getMessage(), e);
                 model.addAttribute("errorMessage", "Không thể tự động điền thông tin của bạn do lỗi hệ thống.");
            }
        }

        // 3. Đưa dữ liệu VÀO Model để render View
        // QUAN TRỌNG: Đảm bảo đưa ĐÚNG đối tượng cart (cái lấy trực tiếp từ session) vào model
        // để Thymeleaf sử dụng đúng trạng thái. Điều này sẽ ghi đè lên cái mà @ModelAttribute
        // có thể đã đặt vào trước đó nếu chúng khác nhau.
        model.addAttribute("cart", cart);
        model.addAttribute("orderDto", orderDto);
        model.addAttribute("currentPage", "checkout");

        log.info("Rendering checkout view with cart from session. Items: {}, Amount: {}", cart.getTotalItems(), cart.getTotalAmount());
        log.error("!!! FINAL CHECKOUT CART STATE: totalAmount = {}, totalItems = {}, itemList is null? {}",
                  (cart != null ? cart.getTotalAmount() : "CART_IS_NULL"),
                  (cart != null ? cart.getTotalItems() : "CART_IS_NULL"),
                  (cart != null ? (cart.getItemList() == null) : "CART_IS_NULL"));

        model.addAttribute("cart", cart); // Đảm bảo dòng này vẫn có để cập nhật model
        return "checkout"; // Dòng return gốc
    }

    // ... (Phương thức placeOrder và success giữ nguyên) ...

    /**
     * Xử lý đặt hàng (chỉ COD).
     * Đã bao gồm logging chi tiết hơn.
     */
    @PostMapping("/place-order")
    public String placeOrder(@Valid @ModelAttribute("orderDto") OrderDTO orderDto,
                             BindingResult bindingResult,
                             @ModelAttribute("shoppingCart") CartDTO cart, // Lấy cart từ session/model
                             HttpSession session,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             SessionStatus sessionStatus) { // Inject SessionStatus để xóa cart attribute

        String currentUserEmail = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : "Guest";
        log.info("Processing place order request (COD only). Submitted by: {}. Recipient: {}", currentUserEmail, orderDto.getRecipientName());
        log.debug("Received OrderDTO for placing order: {}", orderDto);
        log.debug("Cart state at place order submission: Items={}, Amount={}",
                 cart != null ? cart.getTotalItems() : "null cart",
                 cart != null ? cart.getTotalAmount() : "null cart");


        model.addAttribute("currentPage", "checkout"); // Set lại nếu trả về view lỗi

        // 1. Kiểm tra validation form
        if (bindingResult.hasErrors()) {
            log.warn("Checkout form validation errors detected!");
            bindingResult.getAllErrors().forEach(error -> log.warn("Validation Error: {}", error.toString()));
            // 'cart' và 'orderDto' đã có trong model do @ModelAttribute, không cần add lại
            return "checkout"; // Trả về form với lỗi
        }

        // 2. Kiểm tra lại giỏ hàng lần nữa
        if (cart == null || cart.getItemList() == null || cart.getItemList().isEmpty()) {
            log.error("Cart is null or empty during place order submission. Redirecting to cart page.");
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đã bị trống hoặc có lỗi xảy ra. Vui lòng thử lại.");
            return "redirect:/cart";
        }

        // 3. Lấy user (nếu có)
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
             try {
                 currentUser = userService.findByEmail(authentication.getName())
                                          .orElseThrow(() -> new UsernameNotFoundException("Logged in user not found in DB: " + authentication.getName()));
             } catch (UsernameNotFoundException e){
                  log.error("Critical Error: Logged in user {} not found in DB during order placement.", authentication.getName(), e);
                  model.addAttribute("errorMessage", "Lỗi xác thực tài khoản. Không thể đặt hàng.");
                  // model.addAttribute("cart", cart); // Đã có sẵn
                  // model.addAttribute("orderDto", orderDto); // Đã có sẵn
                  return "checkout"; // Quay lại trang checkout với lỗi
             }
        }

        // 4. Set phương thức thanh toán là COD (đã làm trong form, nhưng set lại ở đây cho chắc)
        orderDto.setPaymentMethod("COD");

        // 5. Đặt hàng
        try {
            log.info("Calling orderService.placeOrder for user: {}", currentUserEmail);
            Order newOrder = orderService.placeOrder(orderDto, cart, currentUser);
            log.info("Order {} placed successfully with COD by user: {}", newOrder.getOrderCode(), currentUserEmail);

            // 6. Xóa giỏ hàng khỏi session
            // Cách 1: Dùng SessionStatus (khuyến nghị khi dùng @SessionAttributes)
            sessionStatus.setComplete();
            // Cách 2: Xóa trực tiếp (nên làm thêm để đảm bảo)
            session.removeAttribute("shoppingCart"); // Sử dụng đúng key "shoppingCart"
            log.info("Shopping cart cleared for session ID: {}", session.getId());

            // 7. Redirect đến trang thành công
            redirectAttributes.addFlashAttribute("orderSuccessMessage", "Đặt hàng thành công! Mã đơn hàng của bạn là: " + newOrder.getOrderCode());
            redirectAttributes.addFlashAttribute("orderCode", newOrder.getOrderCode()); // Nếu trang success cần mã đơn hàng
            log.info("Redirecting to checkout success page for order code: {}", newOrder.getOrderCode());
            return "redirect:/checkout/success";

        } catch (ProductNotFoundException | IllegalArgumentException | IllegalStateException e) {
            // Lỗi nghiệp vụ dự kiến (sản phẩm hết hàng, giỏ trống...)
            log.warn("Business error during place order: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            // 'cart' và 'orderDto' đã có trong model
            return "checkout"; // Quay lại form checkout với lỗi
        } catch (Exception e) {
            // Lỗi không mong muốn
            log.error("Unexpected error during place order processing for user {}: {}", currentUserEmail, e.getMessage(), e);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống trong quá trình đặt hàng. Vui lòng thử lại hoặc liên hệ hỗ trợ.");
            // 'cart' và 'orderDto' đã có trong model
            return "checkout"; // Quay lại form checkout với lỗi
        }
    }

    /**
     * Trang hiển thị đặt hàng thành công.
     */
    @GetMapping("/success")
    public String orderSuccess(Model model) {
        // Chỉ hiển thị nếu có thông báo thành công từ redirect
        if (!model.containsAttribute("orderSuccessMessage")) {
            log.warn("Direct access to /checkout/success without success message. Redirecting to home.");
            return "redirect:/"; // Tránh truy cập trực tiếp
        }
        model.addAttribute("currentPage", "orderSuccess");
        log.info("Displaying order success page.");
        return "order-success"; // --> /templates/order-success.html
    }

    // Các phương thức khác (nếu có)...
}