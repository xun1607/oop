package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users") // Base path cho quản lý người dùng
@PreAuthorize("hasRole('ADMIN')") // Chỉ Admin mới truy cập được
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService; // Inject UserService

    /**
     * Hiển thị danh sách người dùng với phân trang và sắp xếp.
     */
    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, // Mặc định 10 user/trang
            @RequestParam(defaultValue = "userId,asc") String sort, // Mặc định sắp xếp theo ID tăng dần
            Model model) {

        log.info("Admin: Request received for user list: page={}, size={}, sort={}", page, size, sort);

        // 1. Xử lý tham số sắp xếp
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = Sort.Direction.ASC; // Mặc định tăng dần
        if (sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])) {
            direction = Sort.Direction.DESC;
        }

        // 2. Tạo đối tượng Pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // 3. Gọi Service để lấy dữ liệu phân trang
        Page<User> userPage;
        try {
            userPage = userService.findAllUsersPaginated(pageable);
            log.debug("Fetched user page: {} users on page {}, total pages: {}",
                      userPage.getNumberOfElements(), page, userPage.getTotalPages());
        } catch (Exception e) {
            log.error("Error fetching paginated users", e);
            // Trả về trang rỗng nếu có lỗi để tránh lỗi trang trắng
            userPage = Page.empty(pageable);
            model.addAttribute("errorMessage", "Lỗi khi tải danh sách người dùng.");
        }


        // 4. Thêm dữ liệu vào Model cho View
        model.addAttribute("userPage", userPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        // Tạo chiều sắp xếp ngược lại cho link header
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        // model.addAttribute("currentPage", page); // Không dùng cho active link sidebar nữa
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort); // Giữ lại tham số sort gốc cho link phân trang

        // === QUAN TRỌNG: Cho active link sidebar ===
        model.addAttribute("currentPage", "users");

        // 5. Trả về tên view
        return "admin/user/list"; // --> /templates/admin/user/list.html
    }

    // --- Các phương thức khác cho Admin User Management (sẽ thêm sau nếu cần) ---
    // Ví dụ: Xem chi tiết user, form sửa vai trò, xử lý cập nhật vai trò, khóa/mở khóa...
    /*
    @GetMapping("/view/{id}")
    public String viewUser(...) { ... }

    @GetMapping("/edit-role/{id}")
    public String showEditRoleForm(...) { ... }

    @PostMapping("/update-role")
    public String updateUserRole(...) { ... }
    */
}