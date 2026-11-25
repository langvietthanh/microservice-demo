package com.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chạy MỘT lần cho MỖI request, trước khi request tới controller.
 * Nhiệm vụ: đọc access token ở header Authorization, nếu hợp lệ thì báo cho
 * Spring Security biết "request này là của user nào, có những quyền gì".
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Tên attribute dùng để chuyển lý do lỗi sang RestAuthenticationEntryPoint */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Không gửi token -> cho đi tiếp. Nếu API cần đăng nhập, Spring Security sẽ tự
        // trả 401.
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            // 1. Kiểm tra chữ ký và hạn dùng
            Claims claims = jwtService.parse(token);

            // 2. Chỉ chấp nhận access token (không cho dùng refresh token để gọi API)
            if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                throw new JwtException("Không phải access token");
            }

            // 3. Lấy lại user + quyền MỚI NHẤT từ DB
            // (nếu user vừa bị xóa/khóa/đổi quyền thì có hiệu lực ngay ở auth-service).
            // Khác với user-service: user-service không có DB quyền nên chỉ đọc quyền trong
            // token.
            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!userDetails.isEnabled()) {
                throw new JwtException("Tài khoản đã bị khóa");
            }

            // 4. Đặt thông tin người dùng vào SecurityContext -> request được coi là đã xác
            // thực
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token đã hết hạn");
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token không hợp lệ");
        }

        // Token lỗi -> không đặt authentication -> Spring Security trả 401 ở bước sau
        filterChain.doFilter(request, response);
    }
}