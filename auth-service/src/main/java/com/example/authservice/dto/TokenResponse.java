package com.example.authservice.dto;

/** Kết quả trả về khi đăng nhập / đổi token thành công */
public record TokenResponse(
        String tokenType,      // luôn là "Bearer"
        String accessToken,    // dùng để gọi API
        long expiresIn,        // số giây access token còn hiệu lực
        String refreshToken) { // dùng để xin access token mới
}
