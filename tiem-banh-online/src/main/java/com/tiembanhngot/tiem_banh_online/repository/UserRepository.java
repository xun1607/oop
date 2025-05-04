package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // Cần cho việc đăng nhập và kiểm tra trùng lặp
    Optional<User> findByPhoneNumber(String phoneNumber); // Kiểm tra trùng lặp
}
