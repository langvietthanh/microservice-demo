package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Dữ liệu tạo tài khoản mới (chỉ ADMIN được gọi) */
public record UserCreateRequest(
        @NotBlank(message = "fullName không được để trống")
        @Size(max = 100, message = "fullName tối đa 100 ký tự")
        String fullName,

        @NotBlank(message = "username không được để trống")
        @Size(min = 3, max = 50, message = "username từ 3 đến 50 ký tự")
        String username,

        @NotBlank(message = "password không được để trống")
        @Size(min = 6, max = 50, message = "password từ 6 đến 50 ký tự")
        String password,

        @Email(message = "email không đúng định dạng")
        String email,

        // Danh sách mã role, ví dụ ["MANAGER"]. Bỏ trống -> mặc định là EMPLOYEE
        Set<String> roles) {
}
