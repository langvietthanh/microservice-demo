package com.example.authservice.service;

import com.example.authservice.dto.UserCreateRequest;
import com.example.authservice.dto.UserResponse;
import com.example.authservice.dto.UserUpdateRequest;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.ApiException;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ROLE = "EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** Tạo tài khoản mới */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username đã tồn tại: " + request.username());
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        // Luôn mã hóa mật khẩu trước khi lưu
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRoles(resolveRoles(request.roles()));

        return UserResponse.from(userRepository.save(user));
    }

    /** Lấy danh sách tài khoản, sắp xếp theo id tăng dần */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Lấy 1 tài khoản theo id */
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getUserOrThrow(id));
    }

    /** Cập nhật họ tên, email, mật khẩu */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        // Chỉ đổi mật khẩu khi client có gửi mật khẩu mới
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        // saveAndFlush: ghi xuống DB ngay để updatedAt trong kết quả trả về là giá trị
        // mới
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    /** Xóa tài khoản theo id. currentUserId: id của người đang thao tác */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể tự xóa tài khoản của chính mình");
        }
        User user = getUserOrThrow(id);
        // Các dòng trong tbl_user_role và tbl_refresh_token của user này được xóa theo
        userRepository.delete(user);
    }

    // ------------------------------------------------------------------

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy user có id = " + id));
    }

    /** Đổi danh sách mã role (["ADMIN"]) thành danh sách entity Role */
    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<String> codes = (roleCodes == null || roleCodes.isEmpty()) ? Set.of(DEFAULT_ROLE) : roleCodes;
        List<Role> roles = roleRepository.findByCodeIn(codes);
        if (roles.size() != codes.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Role không hợp lệ: " + codes);
        }
        return new HashSet<>(roles);
    }
}