package com.tiembanhngot.tiem_banh_online.controller; // HOẶC PACKAGE PHÙ HỢP

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController { 

    @GetMapping("/tiembanh")
    public String chatPage(Model model) {
        model.addAttribute("currentPage", "chat"); 
        return "chat"; 
    }
}