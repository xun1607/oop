package com.tiembanhngot.tiem_banh_online.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tiembanhngot.tiem_banh_online.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
 // Tuần 1 thường chưa cần query phức tạp ở đây
 // Tuần sau có thể cần tìm items theo Order hoặc Product
 // List<OrderItem> findByOrder(Order order);
}