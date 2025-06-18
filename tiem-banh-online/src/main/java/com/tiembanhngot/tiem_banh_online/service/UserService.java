package com.tiembanhngot.tiem_banh_online.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import com.tiembanhngot.tiem_banh_online.dto.UserRegisterDTO;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.entity.User.Role;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;

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
    public User registerNewUser(UserRegisterDTO userDto, BindingResult bindingResult) throws IllegalArgumentException {
        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(userDto.getEmail())) {
            bindingResult.rejectValue("email", "email.exists", "Địa chỉ email này đã được sử dụng.");
        }
        // 2. Kiểm tra SĐT đã tồn tại chưa (nếu có nhập)
        if (StringUtils.hasText(userDto.getPhoneNumber()) &&
            userRepository.existsByPhoneNumber(userDto.getPhoneNumber()) ){
            bindingResult.rejectValue("phoneNumber", "phoneNumber.exists", "Số điện thoại này đã được sử dụng.");
        }
        if (bindingResult.hasErrors()) {
            return null; 
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