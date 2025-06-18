package com.tiembanhngot.tiem_banh_online.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tiembanhngot.tiem_banh_online.service.OrderService;
import com.tiembanhngot.tiem_banh_online.service.ProductService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final OrderService orderService;     
    private final ProductService productService; 

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        log.info("Accessing admin dashboard.");

        try {
            long pendingOrdersCount = orderService.countOrdersByStatus("PENDING"); 
            model.addAttribute("newOrdersCount", pendingOrdersCount);
            long totalProductsCount = productService.countTotalProducts();
            model.addAttribute("totalProducts", totalProductsCount);
        } catch (Exception e) {
            log.error("Error fetching dashboard statistics: {}", e.getMessage(), e);
            model.addAttribute("statError", "Không thể tải dữ liệu thống kê: " + e.getMessage());
            model.addAttribute("newOrdersCount", "N/A");
            model.addAttribute("totalProducts", "N/A");
        }
        
        model.addAttribute("pageTitle", "Bảng Điều Khiển");
        model.addAttribute("currentPage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }
}