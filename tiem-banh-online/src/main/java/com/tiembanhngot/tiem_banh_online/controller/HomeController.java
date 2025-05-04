package com.tiembanhngot.tiem_banh_online.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Import Model
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) { // Thêm Model
        model.addAttribute("currentPage", "home"); // Đặt tên trang hiện tại
        return "index";
    }
}