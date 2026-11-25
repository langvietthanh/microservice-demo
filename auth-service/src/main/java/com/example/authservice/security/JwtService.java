package com.example.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Tạo và kiểm tra JWT */
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_AUTHORITIES = "authorities"; // MỚI
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    @Getter
    private final Duration accessTokenTtl;
    @Getter
    private final Duration refreshTokenTtl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
        // Khóa bí mật dạng Base64 -> SecretKey dùng cho thuật toán HMAC-SHA.
        // Khóa này lấy từ application.yml DÙNG CHUNG trên Config Server: Gateway và
        // user-service có cùng khóa.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenTtl = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenDays);
    }

    /** Access token: sống ngắn, gửi kèm mỗi lần gọi API */
    public String generateAccessToken(CustomUserDetails user) {
        Instant now = Instant.now();

        // MỚI: đưa danh sách role + permission vào token, để các service khác
        // (user-service...) tự phân quyền mà KHÔNG cần đọc auth_db
        List<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        return Jwts.builder()
                .subject(user.getUsername()) // sub: token này của ai
                .claim("uid", user.getId())
                .claim(CLAIM_AUTHORITIES, authorities) // MỚI: ["ROLE_MANAGER", "USER_VIEW_LIST"]
                .claim(CLAIM_TYPE, TYPE_ACCESS) // đánh dấu loại token
                .issuedAt(Date.from(now)) // iat: thời điểm cấp
                .expiration(Date.from(now.plus(accessTokenTtl))) // exp: thời điểm hết hạn
                .signWith(key) // ký bằng khóa bí mật
                .compact();
    }

    /**
     * Refresh token: sống dài, chỉ dùng để xin access token mới. tokenId (jti) được
     * lưu trong DB
     */
    public String generateRefreshToken(String username, String tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .id(tokenId) // jti: mã định danh của token
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenTtl)))
                .signWith(key)
                .compact();
    }

    /**
     * Kiểm tra chữ ký + hạn dùng rồi trả về nội dung (claims) của token.
     * Ném ExpiredJwtException nếu hết hạn, JwtException nếu token sai/bị sửa.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}