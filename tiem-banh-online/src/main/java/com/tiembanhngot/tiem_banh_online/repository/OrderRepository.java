package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.Order;
import com.tiembanhngot.tiem_banh_online.entity.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // **Import cho Specification**
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
// import java.util.List; // Bỏ nếu không dùng List trực tiếp
import java.util.Optional;

@Repository
// **Kế thừa JpaSpecificationExecutor để dùng Specification lọc**
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    // **Thêm:** Tìm đơn hàng của user và phân trang
    Page<Order> findByUser(User user, Pageable pageable);
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems oi JOIN FETCH oi.product WHERE o.orderId = :id")
    Optional<Order> findByIdWithItemsAndProducts(@Param("id") Long id);

    // Các phương thức cũ/khác nếu có...
    // List<Order> findByUserOrderByCreatedAtDesc(User user);
    // List<Order> findByStatus(String status);
    // List<Order> findAllByOrderByCreatedAtDesc();
}