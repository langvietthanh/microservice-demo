package com.example.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.authservice.dto.ErrorResponse;

import java.util.stream.Collectors;

/**
 * Bắt mọi exception ném ra từ controller/service và đổi thành JSON
 * ErrorResponse
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Lỗi nghiệp vụ: 404 không tìm thấy, 409 trùng username... */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    /** Dữ liệu đầu vào sai (@NotBlank, @Size...) -> 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Body không phải JSON hợp lệ -> 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Body không đúng định dạng JSON", request);
    }

    /** Tham số trên URL sai kiểu, ví dụ /api/accounts/abc -> 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Tham số " + ex.getName() + " không hợp lệ", request);
    }

    /** Đăng nhập thất bại -> 401 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        String message = "Xác thực thất bại";
        if (ex instanceof BadCredentialsException) {
            // Không nói rõ sai username hay sai password để tránh bị dò tài khoản
            message = "Sai username hoặc password";
        } else if (ex instanceof DisabledException) {
            message = "Tài khoản đã bị khóa";
        }
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    /**
     * Đã đăng nhập nhưng không đủ quyền (@PreAuthorize trả về false) -> 403.
     * Phải bắt riêng ở đây, nếu không hàm handleOther bên dưới sẽ "nuốt" mất và trả
     * về 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện chức năng này", request);
    }

    /** Các lỗi còn lại -> 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest request) {
        // Lỗi có sẵn mã HTTP của Spring MVC (sai URL -> 404, sai method -> 405...): giữ
        // nguyên mã đó
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, status.getReasonPhrase(), request);
        }
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI()));
    }
}