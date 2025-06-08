package com.tiembanhngot.tiem_banh_online.repository;

import com.tiembanhngot.tiem_banh_online.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); 
    Optional<User> findByPhoneNumber(String phoneNumber); 
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
