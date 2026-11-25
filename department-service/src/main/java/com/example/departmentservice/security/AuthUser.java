package com.example.departmentservice.security;

/**
 * Người dùng đang gọi API, dựng lại từ access token.
 * user-service KHÔNG có bảng user/role/permission của auth-service
 * -> mọi thông tin về người gọi đều lấy từ token (đã được kiểm tra chữ ký).
 */
public class AuthUser {

    private final Long id; // claim "uid": dùng khi cần kiểm tra "có phải chính mình không"
    private final String username; // claim "sub"

    public AuthUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}