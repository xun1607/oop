package com.tiembanhngot.tiem_banh_online.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tiembanhngot.tiem_banh_online.dto.UserRegisterDTO;
import com.tiembanhngot.tiem_banh_online.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AuthController {
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) { 
        model.addAttribute("currentPage", "login");
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        try {
            model.addAttribute("userDto", new UserRegisterDTO());
        } catch (Exception e) {
            log.error("Error creating new UserRegisterDTO", e);
            model.addAttribute("errorMessage", "Lỗi khởi tạo form đăng ký.");
            return "error";
        }

        model.addAttribute("currentPage", "register");
        return "register";
    }

@PostMapping("/register")
public String processRegistration(@Valid @ModelAttribute("userDto") UserRegisterDTO userDto,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
    model.addAttribute("currentPage", "register");

    try {
        if (!bindingResult.hasErrors()) {
            userService.registerNewUser(userDto, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        redirectAttributes.addFlashAttribute("registrationSuccess", "Đăng ký tài khoản thành công!<br> Vui lòng đăng nhập.");
        return "redirect:/login";
    } catch (Exception e) {
        model.addAttribute("registrationError", "Đã xảy ra lỗi khi đăng ký: " + e.getMessage());
        return "register";
    }
}

}