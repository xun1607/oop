package com.tiembanhngot.tiem_banh_online.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.OrderService;
import com.tiembanhngot.tiem_banh_online.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/account")
@PreAuthorize("isAuthenticated()")
@Slf4j
public class AccountController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    private User getCurrentUser(Authentication auth){
        return userService.findByEmail(auth.getName())
                            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + auth.getName()));
    }

    @GetMapping
    public String accountOverview(Authentication auth, Model model) {
        User currentUser = getCurrentUser(auth);
        model.addAttribute("user", currentUser);
        model.addAttribute("currentPage", "account");
        return "account/overview"; 
    }

    @GetMapping("/orders")
    public String listUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth,
            Model model) {

        User currentUser = getCurrentUser(auth);
        log.info("User {} is viewing their order history (page: {}, size: {})", currentUser.getEmail(), page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderService.findOrdersByUserPaginated(currentUser, pageable);

        model.addAttribute("orderPage", orders);
        model.addAttribute("currentPage", "account-orders");
        model.addAttribute("user", currentUser);

        return "account/orders"; 
    }

    @GetMapping("/orders/{orderId}") 
    public String viewUserOrderDetail(@PathVariable Long orderId, Authentication auth, Model model) { 
        User currentUser = getCurrentUser(auth);

        Order order = orderService.findOrderDetailsById(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        if (!order.getUser().getUserId().equals(currentUser.getUserId())) {
            log.warn("User {} attempted to access order ID {} which does not belong to them.", currentUser, orderId);
            throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này.");
        }

        log.debug("User {} is viewing details of order ID {}", currentUser.getEmail(), orderId);

        model.addAttribute("order", order);
        model.addAttribute("currentPage", "account-orders"); 
        
        return "account/order-detail"; 
    }

}