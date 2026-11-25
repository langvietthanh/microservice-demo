package com.example.authservice.dto;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/** Định dạng lỗi thống nhất cho mọi API (Gateway và user-service dùng cùng định dạng này) */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,       // mã HTTP: 400, 401, 403, 404, 409...
        String error,     // tên lỗi: Bad Request, Unauthorized, Forbidden...
        String message,   // mô tả cho người dùng
        String path,      // API đã gọi
        String source) {  // MỚI: thành phần nào trả lỗi

    /** Tên container đang chạy (Docker tự đặt biến môi trường HOSTNAME) */
    private static final String SOURCE = System.getenv().getOrDefault("HOSTNAME", "auth-service");

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, path, SOURCE);
    }
}
