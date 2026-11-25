package com.example.authservice.dto;

import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Thông tin tài khoản trả về cho client. Tuyệt đối KHÔNG trả về password. */
public record UserResponse(
        Long id,
        String fullName,
        String username,
        String email,
        boolean enabled,
        Set<String> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Chuyển entity User -> UserResponse */
    public static UserResponse from(User user) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                roleCodes,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
