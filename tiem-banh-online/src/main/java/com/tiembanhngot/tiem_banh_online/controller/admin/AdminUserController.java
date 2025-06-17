package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId,asc") String sort,
            Model model) {

        log.info("Admin: Listing users - page={}, size={}, sort={}", page, size, sort);

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = (sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<User> userPage;
        try {
            userPage = userService.findAllUsersPaginated(pageable);
            log.debug("Fetched {} users on page {}, total pages: {}",
                      userPage.getNumberOfElements(), page, userPage.getTotalPages());
        } catch (Exception e) {
            log.error("Failed to fetch paginated users", e);
            userPage = Page.empty(pageable);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách người dùng.");
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", "users"); 

        return "admin/user/list";
    }

    // Các chức năng quản lý người dùng nâng cao sẽ thêm sau (edit role, view detail, lock/unlock...)
}