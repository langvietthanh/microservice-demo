package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/** Dữ liệu client gửi lên khi đổi token mới hoặc đăng xuất */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken không được để trống") String refreshToken) {
}
