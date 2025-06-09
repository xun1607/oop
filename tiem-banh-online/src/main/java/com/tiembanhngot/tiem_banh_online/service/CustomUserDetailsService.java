package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collection;
import java.util.Collections; // Import Collections


@Service // Đánh dấu là Service Bean
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // Chỉ đọc từ DB
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tìm user trong database bằng email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Lấy vai trò của user và chuyển thành GrantedAuthority
        // Giả sử User có thuộc tính 'role' là đối tượng Role với phương thức getRoleName()
        // Và Role có tên dạng 'admin', 'customer'
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()) // Quan trọng: Thêm tiền tố ROLE_
        );


        // Trả về đối tượng UserDetails mà Spring Security hiểu
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Username (dùng email)
                user.getPasswordHash(), // Mật khẩu đã được hash trong DB
                authorities // Danh sách quyền
        );

        // Các thuộc tính khác của UserDetails (enabled, accountNonExpired, etc.)
        // có thể được set nếu User entity của bạn có các cột tương ứng
        /*
        return new org.springframework.security.core.userdetails.User(
                 user.getEmail(),
                 user.getPasswordHash(),
                 user.isEnabled(), // ví dụ
                 user.isAccountNonExpired(), // ví dụ
                 user.isCredentialsNonExpired(), // ví dụ
                 user.isAccountNonLocked(), // ví dụ
                 authorities);
        */
    }
    
}