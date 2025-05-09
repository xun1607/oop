package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.OrderDTO;
import com.tiembanhngot.tiem_banh_online.entity.*;
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate; // Import cho WebSocket
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// DTO cho thông báo WebSocket (có thể đặt trong package dto)
@lombok.Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor
class NewOrderNotificationDTO {
    private Long orderId;
    private String orderCode;
    private String customerName;
    private BigDecimal totalAmount;
}

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SimpMessagingTemplate messagingTemplate; // Inject để gửi WebSocket

    // --- Đặt hàng ---
    @Transactional
    public Order placeOrder(OrderDTO orderDto, CartDTO cart, User currentUser) {
        if (cart == null || cart.getItemList().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng.");
        }

        Order order = new Order();
        order.setRecipientName(orderDto.getRecipientName());
        order.setRecipientPhone(orderDto.getRecipientPhone());
        order.setShippingAddress(orderDto.getShippingAddress());
        order.setNotes(orderDto.getNotes());
        order.setUser(currentUser); // Có thể null nếu là khách
        order.setOrderCode("HD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        for (var cartItem : cart.getItemList()) {
            OrderItem orderItem = new OrderItem();
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Sản phẩm ID " + cartItem.getProductId() + " không tồn tại khi đặt hàng."));
             if (!product.getIsAvailable()){
                 throw new IllegalArgumentException("Sản phẩm '" + product.getName() + "' hiện không có sẵn.");
             }
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPrice()); // Lấy giá từ CartItemDTO
            orderItem.setSizeAtPurchase(cartItem.getSelectedSize());
            order.addOrderItem(orderItem);
            log.debug("Added OrderItem: ProductId={}, Qty={}, Price={}, Size={}",
                      orderItem.getProduct().getProductId(), orderItem.getQuantity(),
                      orderItem.getPriceAtPurchase(), orderItem.getSizeAtPurchase());
        }

        order.setTotalAmount(cart.getTotalAmount());
        order.setShippingFee(calculateShippingFee(orderDto)); // Tính phí ship
        order.setFinalAmount(order.getTotalAmount().add(order.getShippingFee()));
        order.setStatus("PENDING"); // Trạng thái chờ xử lý
        order.setPaymentMethod(orderDto.getPaymentMethod());
        order.setPaymentStatus("UNPAID");
        

        Order savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with code: {}", savedOrder.getOrderCode());

        // **Gửi thông báo WebSocket cho Admin**
        try {
            NewOrderNotificationDTO notification = new NewOrderNotificationDTO(
                savedOrder.getOrderId(),
                savedOrder.getOrderCode(),
                currentUser != null ? currentUser.getFullName() : orderDto.getRecipientName(), // Ưu tiên tên user nếu có
                savedOrder.getFinalAmount()
            );
            messagingTemplate.convertAndSend("/topic/admin/new-orders", notification);
            log.info("Sent new order WebSocket notification for order ID: {}", savedOrder.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for new order ID: {}", savedOrder.getOrderId(), e);
        }

        return savedOrder;
    }

     private BigDecimal calculateShippingFee(OrderDTO orderDto) {
         // TODO: Implement logic tính phí ship phức tạp hơn nếu cần
         return new BigDecimal("25000"); // Phí ship cố định ví dụ
     }

    // --- Các phương thức cho Admin ---
    @Transactional(readOnly = true)
    public Page<Order> findAllOrdersPaginated(Pageable pageable) {
        log.debug("Admin: Fetching all orders with pagination: {}", pageable);
        if (pageable.getSort().isUnsorted()) {
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findOrdersFilteredAndPaginated(String status, Pageable pageable) {
         log.debug("Admin: Fetching orders with filter (status='{}') and pagination: {}", status, pageable);
         Specification<Order> spec = (root, query, cb) -> {
             if (StringUtils.hasText(status)) {
                 return cb.equal(root.get("status"), status);
             }
             return cb.conjunction(); // Trả về điều kiện luôn đúng nếu không có status filter
         };
         if (pageable.getSort().isUnsorted()) {
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
         }
         return orderRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findOrderDetailsById(Long id) {
        log.debug("Admin: Fetching order details for ID: {}", id);
        // Lấy order và fetch items cùng lúc để tránh LazyInitException
        // Cách 1: Dùng findById và truy cập items trong transaction (service/controller) - Đơn giản nhất
         Optional<Order> orderOpt = orderRepository.findById(id);
         // Cách 2: Thêm method vào Repository với JOIN FETCH hoặc @EntityGraph
         // Optional<Order> orderOpt = orderRepository.findByIdAndFetchItemsEagerly(id);
         orderOpt.ifPresent(order -> {
             // Touch a field to potentially initialize lazy collection if needed outside TX
             // log.trace("Number of items in order {}: {}", id, order.getOrderItems().size());
         });
        return orderOpt;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        log.info("Admin: Attempting to update status for order ID: {} to {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
             throw new IllegalArgumentException("Không thể cập nhật trạng thái từ '" + order.getStatus() + "' sang '" + newStatus + "'.");
        }

        log.info("Updating order status from {} to {}", order.getStatus(), newStatus);
        order.setStatus(newStatus);

        // Logic cập nhật trạng thái thanh toán tự động
        if ("DELIVERED".equalsIgnoreCase(newStatus) && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                 order.setPaymentStatus("PAID");
                 log.info("Order ID: {} payment status automatically updated to PAID for COD.", orderId);
            } else {
                 log.warn("Order ID: {} delivered but payment status is not PAID and method is not COD.", orderId);
                 // Có thể cần hành động khác ở đây
            }
        } else if ("CANCELLED".equalsIgnoreCase(newStatus) && !"REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                 order.setPaymentStatus("REFUNDED"); // Giả sử hoàn tiền khi hủy đơn đã thanh toán
                 log.info("Order ID: {} payment status updated to REFUNDED due to cancellation.", orderId);
            } else {
                 // Nếu chưa thanh toán, có thể giữ nguyên UNPAID hoặc đặt là CANCELLED/VOID
                 log.info("Order ID: {} cancelled while payment status was {}.", orderId, order.getPaymentStatus());
            }
            // TODO: Thêm logic hoàn trả số lượng sản phẩm vào kho (nếu cần)
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order ID: {} status successfully updated to {}. Payment status: {}", orderId, updatedOrder.getStatus(), updatedOrder.getPaymentStatus());
        return updatedOrder;
    }

    // Kiểm tra chuyển trạng thái
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (!StringUtils.hasText(currentStatus) || !StringUtils.hasText(newStatus)) return false;
        if (currentStatus.equalsIgnoreCase(newStatus)) return false; // Không cho cập nhật trạng thái giống hệt

        // Quy tắc cơ bản: không đổi trạng thái sau khi DELIVERED hoặc CANCELLED
        if ("CANCELLED".equalsIgnoreCase(currentStatus) || "DELIVERED".equalsIgnoreCase(currentStatus)) {
            log.warn("Attempted invalid status transition from {} to {}", currentStatus, newStatus);
             return false;
        }
        // Thêm các quy tắc khác nếu cần, ví dụ:
        // if ("PENDING".equalsIgnoreCase(currentStatus) && "SHIPPED".equalsIgnoreCase(newStatus)) return false; // Phải qua PROCESSING trước

        return true;
    }

    // --- Các phương thức cho User ---
    @Transactional(readOnly = true)
    public Page<Order> findOrdersByUserPaginated(User user, Pageable pageable) {
        log.debug("Fetching orders for user ID: {} with pagination: {}", user.getUserId(), pageable);
        if (pageable.getSort().isUnsorted()) {
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findByUser(user, pageable); // Đảm bảo method này có trong OrderRepository
    }
    @lombok.Data @lombok.AllArgsConstructor @lombok.NoArgsConstructor
    class NewOrderNotificationDTO {
        private Long orderId;
        private String orderCode;
        private String customerName;
        private BigDecimal totalAmount;
    }
}