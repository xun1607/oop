package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.dto.UserRegisterDTO;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.entity.User.Role;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; 

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
   
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional 
    public User registerNewUser(UserRegisterDTO userDto) throws IllegalArgumentException {
        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Địa chỉ email đã được sử dụng.");
        }

        // 2. Kiểm tra SĐT đã tồn tại chưa (nếu có nhập)
        if (StringUtils.hasText(userDto.getPhoneNumber()) &&
            userRepository.existsByPhoneNumber(userDto.getPhoneNumber()) ){
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
        }

        // 3. Tạo user mới
        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setFullName(userDto.getFullName());
        newUser.setPhoneNumber(userDto.getPhoneNumber());
        newUser.setRole(Role.CUSTOMER);
        newUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));

        return userRepository.save(newUser);
    }


    @Transactional(readOnly = true) 
    public Page<User> findAllUsersPaginated(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

}