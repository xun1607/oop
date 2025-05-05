package com.tiembanhngot.tiem_banh_online.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        log.info("Accessing admin dashboard.");
        // Thêm dữ liệu thống kê vào model nếu cần
        model.addAttribute("pageTitle", "Bảng Điều Khiển");
        model.addAttribute("currentPage", "dashboard"); // <-- QUAN TRỌNG
        return "admin/dashboard";
    }

    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }
}