package com.tiembanhngot.tiem_banh_online.controller;

// --- Các import hiện có ---
import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.service.OrderService;
import com.tiembanhngot.tiem_banh_online.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// --- THÊM CÁC IMPORT SAU ---
import org.springframework.web.bind.annotation.PathVariable;         // Cho @PathVariable
import org.springframework.security.access.AccessDeniedException; // Cho lỗi phân quyền
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException; // Cho lỗi không tìm thấy Order

@Controller
@RequestMapping("/account")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final UserService userService;
    private final OrderService orderService;

    // Trang thông tin tài khoản chính
    @GetMapping
    public String accountOverview(Authentication authentication, Model model) {
        String username = authentication.getName();
        User currentUser = userService.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        model.addAttribute("user", currentUser);
        model.addAttribute("currentPage", "account");
        log.debug("Displaying account overview for user: {}", username);
        // Tạo file view này nếu chưa có
        return "account/overview"; // --> /templates/account/overview.html
    }

    // Trang lịch sử đơn hàng
    @GetMapping("/orders")
    public String listUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            Model model) {

        String username = authentication.getName();
        log.info("Fetching orders for user: {}, page: {}, size: {}", username, page, size);

        User currentUser = userService.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderService.findOrdersByUserPaginated(currentUser, pageable);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("currentPage", "account-orders");
        model.addAttribute("user", currentUser);

        log.debug("Returning order history view for user {}.", username);
        // Tạo file view này nếu chưa có
        return "account/orders"; // --> /templates/account/orders.html
    }

     // Trang chi tiết đơn hàng của người dùng
     @GetMapping("/orders/{orderId}") // Sử dụng @PathVariable ở đây
    public String viewUserOrderDetail(@PathVariable Long orderId, Authentication authentication, Model model) { // Thêm @PathVariable
        String username = authentication.getName();
        User currentUser = userService.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

         // Sử dụng OrderNotFoundException đã import
         Order order = orderService.findOrderDetailsById(orderId)
             .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

         // Kiểm tra quyền xem đơn hàng
         if (order.getUser() == null || !order.getUser().getUserId().equals(currentUser.getUserId())) {
              log.warn("User {} attempted to access order ID {} which does not belong to them.", username, orderId);
              // Sử dụng AccessDeniedException đã import
              throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này.");
         }

         model.addAttribute("order", order);
         model.addAttribute("currentPage", "account-orders"); // Giữ highlight menu
         log.debug("Displaying order detail view for order ID {} for user {}", orderId, username);
         // Tạo file view này nếu chưa có
         return "account/order-detail"; // --> /templates/account/order-detail.html
    }

}