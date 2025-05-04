package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
//Import thêm nếu cần (ví dụ List)

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
 // Tuần 1 thường chưa cần query phức tạp ở đây
 // Tuần sau có thể cần tìm items theo Order hoặc Product
 // List<OrderItem> findByOrder(Order order);
}