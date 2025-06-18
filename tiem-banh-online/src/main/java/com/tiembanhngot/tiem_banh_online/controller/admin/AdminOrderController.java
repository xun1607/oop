package com.tiembanhngot.tiem_banh_online.controller.admin;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {
    @Autowired
    private OrderService orderService;

    // Danh sách các trạng thái đơn hàng có thể có
    private static final List<String> ORDER_STATUSES = Arrays.asList("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    @GetMapping
    public String listOrders(   // danh sách các orders
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort, // sap xet orders từ mới nhất tới cũ hơn
            @RequestParam(required = false) String status, 
            Model model) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1]) : Sort.Direction.DESC;
        String sortField = sortParams[0];
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Order> orderPage = orderService.findOrdersFilteredAndPaginated(status, pageable); // logic lọc và phân trang

        model.addAttribute("orderPage", orderPage);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", direction.name());
        model.addAttribute("reverseSortDir", direction == Sort.Direction.ASC ? "desc" : "asc");
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("sort", sort);
        model.addAttribute("statusFilter", status);
        model.addAttribute("orderStatuses", ORDER_STATUSES); // Đưa danh sách trạng thái cho dropdown lọc

        return "admin/order/list";
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try { //tìm order bằng orderID
            Order order = orderService.findOrderDetailsById(id)
                    .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng với ID: " + id));

            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", ORDER_STATUSES);


            if (order != null) {
                log.error("   -> finalAmount from Order object (Admin): {}", order.getFinalAmount());
                log.error("   -> orderItems size (Admin): {}", order.getOrderItems() != null ? order.getOrderItems().size() : "NULL");
            }
            
            return "admin/order/detail";

        } catch (OrderNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải chi tiết đơn hàng: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/update-status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") String newStatus,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đơn hàng ID: " + orderId + " thành " + newStatus);
        } catch (OrderNotFoundException | IllegalArgumentException e) {
            log.warn("Admin: Failed to update status for order ID {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("Admin: Unexpected error updating status for order ID {}", orderId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi không mong muốn khi cập nhật trạng thái đơn hàng.");
        }
        return "redirect:/admin/orders/" + orderId;
    }

}