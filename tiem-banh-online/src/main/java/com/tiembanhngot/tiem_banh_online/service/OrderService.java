package com.tiembanhngot.tiem_banh_online.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tiembanhngot.tiem_banh_online.dto.CartDTO;
import com.tiembanhngot.tiem_banh_online.dto.OrderDTO;
import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.entity.OrderItem;
import com.tiembanhngot.tiem_banh_online.entity.Product;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.exception.OrderNotFoundException;
import com.tiembanhngot.tiem_banh_online.exception.ProductNotFoundException;
import com.tiembanhngot.tiem_banh_online.repository.OrderRepository;
import com.tiembanhngot.tiem_banh_online.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@lombok.Data 
@lombok.AllArgsConstructor 
@lombok.NoArgsConstructor
class NewOrderNotificationDTO {
    private Long orderId;
    private String orderCode;
    private String customerName;
    private BigDecimal totalAmount;
}

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate; 

    @Transactional
    public Order placeOrder(OrderDTO orderDto, CartDTO cart, User currentUser) {
        if (cart == null || cart.getItemList().isEmpty()) {
            throw new IllegalStateException("The cart is empty, unable to place an order.");
        }

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
                    .orElseThrow(() -> new ProductNotFoundException("Product ID " + cartItem.getProductId() + " is empty."));
            if (!product.getIsAvailable()){
                throw new IllegalArgumentException("Product '" + product.getName() + "' currently unavailable.");
            }
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPrice()); 
            orderItem.setPriceAtPurchase(cartItem.getPrice()); 
            orderItem.setSizeAtPurchase(cartItem.getSelectedSize());
            order.addOrderItem(orderItem);
            
        }

        order.setTotalAmount(cart.getTotalAmount());
        order.setShippingFee(calculateShippingFee(orderDto)); 
        order.setFinalAmount(order.getTotalAmount().add(order.getShippingFee()));
        order.setStatus("PENDING"); 
        order.setPaymentStatus("UNPAID");
        
        Order savedOrder = orderRepository.save(order);
    
        try {
            NewOrderNotificationDTO notification = new NewOrderNotificationDTO(
                savedOrder.getOrderId(),
                savedOrder.getOrderCode(),
                currentUser != null ? currentUser.getFullName() : orderDto.getRecipientName(), 
                savedOrder.getFinalAmount()
            );
            messagingTemplate.convertAndSend("/topic/admin/new-orders", notification);
            
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for new order ID: {}", savedOrder.getOrderId(), e);
        }

        return savedOrder;
    }

    private BigDecimal calculateShippingFee(OrderDTO orderDto) {
        return new BigDecimal("25000");
    }

    @Transactional(readOnly = true)
    public Page<Order> findAllOrdersPaginated(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> findOrdersFilteredAndPaginated(String status, Pageable pageable) {
        
        Specification<Order> spec = (root, query, cb) -> {
            if (StringUtils.hasText(status)) {
                return cb.equal(root.get("status"), status);
            }
            return cb.conjunction();
        };
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return orderRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findOrderDetailsById(Long id) {  //tim thong tin order bang id
        Optional<Order> orderOpt = orderRepository.findById(id);
        orderOpt.ifPresent(order -> {
        });
        return orderOpt;
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, String newStatus) {
        log.info("Admin: Updating order ID {} to status {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found."));

        if (!isValidStatusTransition(order.getStatus(), newStatus)) {
            throw new IllegalArgumentException("Cannot change status from '" + order.getStatus() + "' to '" + newStatus + "'.");
        }

        order.setStatus(newStatus);

        switch (newStatus.toUpperCase()) {
            case "DELIVERED":
                if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                    order.setPaymentStatus("PAID");
                }
                break;

            case "CANCELLED":
                if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setPaymentStatus("REFUNDED");
                } else {
                    log.info("Order ID {} cancelled with payment status {}.", orderId, order.getPaymentStatus());
                }
                break;
        }

        Order updated = orderRepository.save(order);
        return updated;
    }

    // Check transition
    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (!StringUtils.hasText(currentStatus) || !StringUtils.hasText(newStatus)) return false;
        if (currentStatus.equalsIgnoreCase(newStatus)) return false; 

        // không đổi trạng thái sau khi DELIVERED hoặc CANCELLED
        if ("CANCELLED".equalsIgnoreCase(currentStatus) || "DELIVERED".equalsIgnoreCase(currentStatus)) {
            log.warn("Attempted invalid status transition from {} to {}", currentStatus, newStatus);
            return false;
        }
        return true;
    }

    // Các phương thức cho User
    @Transactional(readOnly = true)
    public Page<Order> findOrdersByUserPaginated(User user, Pageable pageable) {
        log.debug("Fetching orders for user ID: {} with pagination: {}", user.getUserId(), pageable);
        return orderRepository.findByUser(user, pageable);
    }
    public long countOrdersByStatus(String status) {
        return orderRepository.countByStatusIgnoreCase(status); 
    }
    
    public long countTotalProducts() {
        return productRepository.count();
    }
}