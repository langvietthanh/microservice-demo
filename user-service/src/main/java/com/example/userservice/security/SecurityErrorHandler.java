package com.example.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trả lỗi bảo mật dạng JSON, cùng định dạng với auth-service và Gateway:
 * - 401: chưa xác thực (thiếu token / token sai / hết hạn)
 * - 403: đã xác thực nhưng không đủ quyền (@PreAuthorize trả về false)
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 401 */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthFilter.AUTH_ERROR_ATTRIBUTE);
        write(request, response, HttpStatus.UNAUTHORIZED,
                reason != null ? reason.toString() : "Bạn chưa đăng nhập");
    }

    /** 403 */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện chức năng này");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        // Instance nào trả lỗi: user-service-1 hay user-service-2 (Docker đặt biến
        // HOSTNAME)
        body.put("source", System.getenv().getOrDefault("HOSTNAME", "user-service"));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}