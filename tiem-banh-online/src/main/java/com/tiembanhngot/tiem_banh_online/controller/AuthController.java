package com.tiembanhngot.tiem_banh_online.controller;

import com.tiembanhngot.tiem_banh_online.dto.UserRegisterDTO;
import com.tiembanhngot.tiem_banh_online.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Import Model
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) { // Thêm Model
        model.addAttribute("currentPage", "login"); // Đặt tên trang
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        log.debug("Serving registration page request."); // Thêm log để kiểm tra
        model.addAttribute("currentPage", "register");
        // Dòng này có thể gây lỗi nếu UserRegisterDTO có vấn đề khởi tạo
        try {
             model.addAttribute("userDto", new UserRegisterDTO());
             log.debug("UserRegisterDTO added to model.");
        } catch (Exception e) {
             log.error("Error creating new UserRegisterDTO", e);
             // Có thể trả về trang lỗi hoặc thông báo lỗi
             model.addAttribute("errorMessage", "Lỗi khởi tạo form đăng ký.");
             return "error"; // Hoặc một trang lỗi tùy chỉnh
        }
        return "register"; // Trả về tên view "register"
    }

     // Phương thức POST không cần add model vì nó redirect hoặc trả về view với model đã có lỗi
    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("userDto") UserRegisterDTO userDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) { // Giữ Model để trả về nếu lỗi

         model.addAttribute("currentPage", "register"); // Set lại phòng khi trả về view lỗi

        // ... (logic xử lý đăng ký như cũ) ...
         if (bindingResult.hasErrors()) {
             return "register";
         }
          // ... (try-catch như cũ) ...
          try {
             userService.registerNewUser(userDto);
             redirectAttributes.addFlashAttribute("registrationSuccess", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
             return "redirect:/login";
          } catch (IllegalArgumentException e) {
              // ... (xử lý lỗi như cũ) ...
              return "register";
          } catch (Exception e){
               // ... (xử lý lỗi như cũ) ...
               model.addAttribute("registrationError", "Đã xảy ra lỗi hệ thống...");
               return "register";
          }
    }
}