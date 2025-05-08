package com.tiembanhngot.tiem_banh_online.service;

import com.tiembanhngot.tiem_banh_online.dto.UserRegisterDTO;
import com.tiembanhngot.tiem_banh_online.entity.Role;
import com.tiembanhngot.tiem_banh_online.entity.User;
import com.tiembanhngot.tiem_banh_online.repository.RoleRepository;
import com.tiembanhngot.tiem_banh_online.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.crypto.password.PasswordEncoder; // **Import**
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // **Import**

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder; // **Inject PasswordEncoder**

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
   
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional // **Đảm bảo transaction**
    public User registerNewUser(UserRegisterDTO userDto) throws IllegalArgumentException {
        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Địa chỉ email đã được sử dụng.");
        }

        // 2. Kiểm tra SĐT đã tồn tại chưa (nếu có nhập)
        if (StringUtils.hasText(userDto.getPhoneNumber()) &&
            userRepository.findByPhoneNumber(userDto.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
        }

        // 3. Tìm vai trò "customer"
        Role customerRole = roleRepository.findByRoleName("customer")
                .orElseThrow(() -> new IllegalStateException("Lỗi hệ thống: Vai trò 'customer' không tồn tại. Vui lòng liên hệ quản trị viên."));

        // 4. Tạo đối tượng User mới
        User newUser = new User();
        newUser.setEmail(userDto.getEmail());
        newUser.setFullName(userDto.getFullName());
        newUser.setPhoneNumber(userDto.getPhoneNumber());
        newUser.setRole(customerRole);

        // 5. **Mã hóa mật khẩu**
        newUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));

        // 6. Lưu vào cơ sở dữ liệu
        return userRepository.save(newUser);
    }

    // === THÊM PHƯƠNG THỨC NÀY ===
    /**
     * Lấy danh sách tất cả người dùng có phân trang và sắp xếp.
     * @param pageable Đối tượng chứa thông tin phân trang và sắp xếp.
     * @return Một trang (Page) chứa danh sách người dùng.
     */
    @Transactional(readOnly = true) // Chỉ đọc dữ liệu
    public Page<User> findAllUsersPaginated(Pageable pageable) {
        // Gọi phương thức findAll của JpaRepository để lấy dữ liệu phân trang
        return userRepository.findAll(pageable);
    }
    // === KẾT THÚC PHẦN THÊM ===

    // Các phương thức khác nếu có...
    // Ví dụ: đếm người dùng (cho dashboard)
    // public long countTotalUsers() {
    //     return userRepository.count();
    // }
}