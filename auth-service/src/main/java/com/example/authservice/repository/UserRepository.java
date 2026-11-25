package com.example.authservice.repository;

import com.example.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Tìm user theo username (dùng khi đăng nhập) */
    Optional<User> findByUsername(String username);

    /** Kiểm tra username đã tồn tại chưa (dùng khi tạo user) */
    boolean existsByUsername(String username);
}