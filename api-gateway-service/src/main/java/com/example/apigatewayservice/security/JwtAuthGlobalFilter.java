package com.example.apigatewayservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lớp bảo vệ thứ nhất: kiểm tra access token NGAY TẠI CỔNG VÀO, cho MỌI route.
 * - Chỉ làm XÁC THỰC (token thật? còn hạn? đúng loại access?) -> sai thì trả
 * 401, request không đi vào trong.
 * - KHÔNG làm phân quyền chi tiết: việc "ai được làm gì" do từng service tự
 * quyết (403).
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    /** Các API không cần access token */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout");

    private static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey key;

    public JwtAuthGlobalFilter(@Value("${app.jwt.secret}") String secret) {
        // CÙNG khóa với auth-service (lấy từ application.yml dùng chung trên Config
        // Server)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. Đường dẫn công khai -> cho qua, không cần token
        if (PUBLIC_PATHS.contains(path)) {
            return chain.filter(exchange);
        }

        // 2. Phải có header "Authorization: Bearer <token>"
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return reject(exchange, "Bạn chưa đăng nhập");
        }

        // 3. Kiểm tra chữ ký + hạn dùng + loại token
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(BEARER_PREFIX.length()))
                    .getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                return reject(exchange, "Access token không hợp lệ"); // không cho dùng refresh token gọi API
            }
        } catch (ExpiredJwtException ex) {
            return reject(exchange, "Access token đã hết hạn");
        } catch (JwtException | IllegalArgumentException ex) {
            return reject(exchange, "Access token không hợp lệ");
        }

        // 4. Token hợp lệ -> chuyển tiếp NGUYÊN request (kể cả header Authorization)
        // xuống service phía sau.
        // Service phía sau sẽ tự kiểm tra lại token và tự phân quyền.
        return chain.filter(exchange);
    }

    /** Chạy sớm, trước các filter định tuyến và load balancing của Gateway */
    @Override
    public int getOrder() {
        return -100;
    }

    /** Trả 401 dạng JSON, cùng định dạng lỗi với auth-service và user-service */
    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"%s","path":"%s","source":"api-gateway-service"}"""
                .formatted(LocalDateTime.now(), message, exchange.getRequest().getURI().getPath());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}