package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.OrderDTO;
import com.tiembanhngot.tiem_banh_online.entity.*;
// **Đảm bảo import từ package exception**
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException; // Import cả cái này nếu cần
import com.tiembanhngot.tiem_banh_online.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest; // **IMPORT ĐÃ THÊM**
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;      // **IMPORT ĐÃ THÊM**
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
// import java.util.List; // Chỉ import nếu dùng
import java.util.Optional;
import java.util.UUID;

// **KHÔNG ĐỊNH NGHĨA EXCEPTION Ở ĐÂY NỮA**

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    // private final UserRepository userRepository;

    // --- Phương thức đặt hàng ---
    @Transactional
    public Order placeOrder(OrderDTO orderDto, CartDTO cart, User currentUser) {
        if (cart == null || cart.getItemList().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống, không thể đặt hàng.");
        }
        // ... (logic đặt hàng giữ nguyên) ...

         Order order = new Order();
        order.setRecipientName(orderDto.getRecipientName());
        order.setRecipientPhone(orderDto.getRecipientPhone());
        order.setShippingAddress(orderDto.getShippingAddress());
        order.setNotes(orderDto.getNotes());
        order.setUser(currentUser);
        order.setOrderCode("HD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        for (var cartItem : cart.getItemList()) {
            OrderItem orderItem = new OrderItem();
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Sản phẩm không tồn tại khi tạo đơn hàng: ID " + cartItem.getProductId())); // Dùng ProductNotFoundException
             if (!product.getIsAvailable()){
                 throw new IllegalArgumentException("Sản phẩm " + product.getName() + " đã hết hàng.");
             }
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPrice());
            order.addOrderItem(orderItem);
        }

        order.setTotalAmount(cart.getTotalAmount());
        order.setShippingFee(calculateShippingFee(orderDto));
        order.setFinalAmount(order.getTotalAmount().add(order.getShippingFee()));
        order.setStatus("PENDING");
        order.setPaymentMethod(orderDto.getPaymentMethod());
        order.setPaymentStatus("UNPAID");

        log.info("Placing new order with code: {}", order.getOrderCode());
        return orderRepository.save(order);
    }

     private BigDecimal calculateShippingFee(OrderDTO orderDto) {
         return new BigDecimal("25000.00");
     }

    // --- Các phương thức cho Admin ---
    @Transactional(readOnly = true)
    public Page<Order> findAllOrdersPaginated(Pageable pageable) {
        log.debug("Admin: Fetching all orders with pagination: {}", pageable);
        if (pageable.getSort().isUnsorted()) {
             // **Sử dụng PageRequest và Sort đã import**
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findOrdersFilteredAndPaginated(String status, Pageable pageable) {
         log.debug("Admin: Fetching orders with filter (status={}) and pagination: {}", status, pageable);
         Specification<Order> spec = Specification.where(null);
         if (StringUtils.hasText(status)) {
             spec = spec.and((root, query, criteriaBuilder) ->
                     criteriaBuilder.equal(root.get("status"), status)
             );
         }
         if (pageable.getSort().isUnsorted()) {
             // **Sử dụng PageRequest và Sort đã import**
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
         }
         return orderRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findOrderDetailsById(Long id) {
        log.debug("Admin: Fetching order details for ID: {}", id);
        return orderRepository.findById(id);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        log.info("Admin: Updating status for order ID: {} to new status: {}", orderId, newStatus);
        Order order = orderRepository.findById(orderId)
                 // **Sử dụng OrderNotFoundException đã import**
                .orElseThrow(() -> new OrderNotFoundException("Không tìm thấy đơn hàng với ID: " + orderId));

        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
             throw new IllegalArgumentException("Không thể cập nhật trạng thái từ '" + order.getStatus() + "' sang '" + newStatus + "'.");
        }
        order.setStatus(newStatus);
        if ("DELIVERED".equalsIgnoreCase(newStatus) && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus("PAID");
            log.info("Order ID: {} payment status updated to PAID due to COD delivery.", orderId);
        }
         if ("CANCELLED".equalsIgnoreCase(newStatus)) {
             order.setPaymentStatus("REFUNDED"); // Hoặc giữ nguyên tùy logic
              log.info("Order ID: {} has been cancelled.", orderId);
         }
        Order updatedOrder = orderRepository.save(order);
        log.info("Order ID: {} status successfully updated to {}", orderId, newStatus);
        return updatedOrder;
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        if (currentStatus.equalsIgnoreCase(newStatus)) return true;
        if ("CANCELLED".equalsIgnoreCase(currentStatus) || "DELIVERED".equalsIgnoreCase(currentStatus)) {
             return false;
        }
        return true;
    }

    // --- Các phương thức cho User ---
    @Transactional(readOnly = true)
    public Page<Order> findOrdersByUserPaginated(User user, Pageable pageable) {
        log.debug("Fetching orders for user ID: {} with pagination: {}", user.getUserId(), pageable);
        if (pageable.getSort().isUnsorted()) {
            // **Sử dụng PageRequest và Sort đã import**
             pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findByUser(user, pageable);
    }
}