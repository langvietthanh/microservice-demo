package com.example.authservice.repository;

import com.example.authservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Lấy các role theo danh sách mã, ví dụ ["ADMIN", "MANAGER"] */
    List<Role> findByCodeIn(Collection<String> codes);
}