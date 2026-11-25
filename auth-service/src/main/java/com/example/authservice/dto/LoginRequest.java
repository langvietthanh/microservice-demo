package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/** Dữ liệu client gửi lên khi đăng nhập */
public record LoginRequest(
        @NotBlank(message = "username không được để trống") String username,
        @NotBlank(message = "password không được để trống") String password) {
}
