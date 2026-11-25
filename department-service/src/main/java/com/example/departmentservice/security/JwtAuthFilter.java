package com.example.departmentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * Lớp bảo vệ thứ hai (zero trust): dù Gateway đã kiểm tra, user-service vẫn tự
 * kiểm tra lại token.
 * Khác auth-service: KHÔNG đọc DB, mà lấy danh sách quyền ngay trong claim
 * "authorities" của token.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Tên attribute dùng để chuyển lý do lỗi sang SecurityErrorHandler */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        // CÙNG khóa với auth-service (lấy từ application.yml dùng chung trên Config
        // Server)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Không gửi token -> cho đi tiếp. API cần đăng nhập sẽ bị Spring Security trả
        // 401.
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Kiểm tra chữ ký + hạn dùng (không gọi auth-service, không đọc DB)
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                    .getPayload();

            // 2. Chỉ chấp nhận access token
            if (!"access".equals(claims.get("type", String.class))) {
                throw new JwtException("Không phải access token");
            }

            // 3. Đọc danh sách quyền NGAY TRONG TOKEN (auth-service đã ghi vào lúc đăng
            // nhập)
            List<?> rawAuthorities = claims.get("authorities", List.class);
            List<SimpleGrantedAuthority> authorities = rawAuthorities == null
                    ? List.of()
                    : rawAuthorities.stream()
                            .map(a -> new SimpleGrantedAuthority(a.toString()))
                            .toList();

            Number uid = claims.get("uid", Number.class);
            AuthUser principal = new AuthUser(uid == null ? null : uid.longValue(), claims.getSubject());

            // 4. Báo cho Spring Security: request này của ai, có quyền gì
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));

        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token đã hết hạn");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Access token không hợp lệ");
        }

        // Token lỗi -> không đặt authentication -> Spring Security trả 401 ở bước sau
        filterChain.doFilter(request, response);
    }
}