package com.tiembanhngot.tiem_banh_online.controller.admin;

import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;

    // Danh sách các trạng thái đơn hàng có thể có
    private static final List<String> ORDER_STATUSES = Arrays.asList("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    @GetMapping
    public String listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort, // Mặc định mới nhất
            @RequestParam(required = false) String status, // Tham số lọc theo trạng thái
            Model model) {

        log.info("Admin: Request received for order list: page={}, size={}, sort={}, status={}", page, size, sort, status);
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.DESC;
        String sortField = sortParams[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Gọi service để lấy đơn hàng đã lọc và phân trang
        Page<Order> orderPage = orderService.findOrdersFilteredAndPaginated(status, pageable);

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);
        model.addAttribute("statusFilter", status); // Giữ lại giá trị lọc
        model.addAttribute("orderStatuses", ORDER_STATUSES); // Đưa danh sách trạng thái cho dropdown lọc

        log.debug("Admin: Returning order list view with {} orders on page {}.", orderPage.getNumberOfElements(), page);
        return "admin/order/list"; // --> /templates/admin/order/list.html
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("Admin: Request received for order detail ID: {}", id);
        try {
            // 1. Gọi Service để lấy Order
            Order order = orderService.findOrderDetailsById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

            // 2. Thêm Order và OrderStatuses vào Model
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", ORDER_STATUSES);

            // 3. Log trước khi trả về View
             log.debug("Admin: Displaying detail for order ID: {}", id);

             // === THÊM LOG KIỂM TRA finalAmount Ở ĐÂY ===
             log.error("!!! FINAL ORDER OBJECT TO VIEW (Admin): {}", order);
             if (order != null) {
                 log.error("   -> finalAmount from Order object (Admin): {}", order.getFinalAmount());
                 log.error("   -> orderItems size (Admin): {}", order.getOrderItems() != null ? order.getOrderItems().size() : "NULL");
             }
             // ==========================================

            // 4. Trả về tên View
            return "admin/order/detail"; // --> /templates/admin/order/detail.html

        } catch (OrderNotFoundException e) {
            // Xử lý nếu không tìm thấy Order -> Redirect về list
            log.warn("Admin: Order not found for viewing details: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/orders";
        } catch (Exception e) { // <-- NÊN THÊM CATCH NÀY
            // Bắt các lỗi khác có thể xảy ra khi lấy dữ liệu (ví dụ: LazyInit)
            log.error("Error fetching order details for ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải chi tiết đơn hàng: " + e.getMessage());
            return "redirect:/admin/orders"; // Redirect về list nếu có lỗi khác
        }
    }

    @PostMapping("/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") String newStatus,
                                    RedirectAttributes redirectAttributes) {
        log.info("Admin: Request received to update status for order ID: {} to {}", orderId, newStatus);
        try {
            orderService.updateOrderStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng ID: " + orderId + " thành " + newStatus);
            log.info("Admin: Successfully updated status for order ID: {}", orderId);
        } catch (OrderNotFoundException | IllegalArgumentException e) {
             log.warn("Admin: Failed to update status for order ID {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Admin: Unexpected error updating status for order ID {}", orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không mong muốn khi cập nhật trạng thái đơn hàng.");
        }
        // Redirect về trang chi tiết đơn hàng hoặc trang danh sách
        return "redirect:/admin/orders/" + orderId;
    }

}