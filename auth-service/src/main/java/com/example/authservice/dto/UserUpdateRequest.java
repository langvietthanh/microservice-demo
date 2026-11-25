package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dữ liệu cập nhật tài khoản.
 * Cố ý KHÔNG có username và roles: employee tự sửa thông tin của mình
 * thì không thể tự nâng quyền cho mình.
 */
public record UserUpdateRequest(
        @NotBlank(message = "fullName không được để trống")
        @Size(max = 100, message = "fullName tối đa 100 ký tự")
        String fullName,

        @Email(message = "email không đúng định dạng")
        String email,

        // Bỏ trống -> giữ nguyên mật khẩu cũ
        @Size(min = 6, max = 50, message = "password từ 6 đến 50 ký tự")
        String password) {
}
