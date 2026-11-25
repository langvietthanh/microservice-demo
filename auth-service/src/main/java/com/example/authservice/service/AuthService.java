package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.TokenResponse;
import com.example.authservice.entity.RefreshToken;
import com.example.authservice.entity.User;
import com.example.authservice.exception.ApiException;
import com.example.authservice.repository.RefreshTokenRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.CustomUserDetails;
import com.example.authservice.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Đăng nhập: kiểm tra username/password, đúng thì cấp cặp access + refresh
     * token
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // AuthenticationManager sẽ gọi CustomUserDetailsService để lấy user,
        // rồi dùng PasswordEncoder so sánh mật khẩu.
        // Sai -> ném BadCredentialsException, tài khoản bị khóa -> DisabledException
        // (GlobalExceptionHandler đổi thành 401).
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.getReferenceById(userDetails.getId());
        return issueTokens(user, userDetails);
    }

    /**
     * Đổi token: nhận refresh token còn hiệu lực, trả về cặp token MỚI.
     * Refresh token cũ bị thu hồi ngay (mỗi refresh token chỉ dùng được 1 lần).
     * Quyền trong access token mới được đọc lại từ DB -> đổi quyền có hiệu lực sau
     * lần refresh.
     */
    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken stored = findValidRefreshToken(refreshTokenValue);

        // Thu hồi token cũ
        stored.setRevoked(true);

        User user = stored.getUser();
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa");
        }
        return issueTokens(user, new CustomUserDetails(user));
    }

    /** Đăng xuất: thu hồi refresh token để không xin được access token mới nữa */
    @Transactional
    public void logout(String refreshTokenValue) {
        RefreshToken stored = findValidRefreshToken(refreshTokenValue);
        stored.setRevoked(true);
    }

    // ------------------------------------------------------------------

    /** Tạo cặp token và lưu jti của refresh token vào DB */
    private TokenResponse issueTokens(User user, CustomUserDetails userDetails) {
        String tokenId = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenId(tokenId);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(jwtService.getRefreshTokenTtl()));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(
                "Bearer",
                jwtService.generateAccessToken(userDetails),
                jwtService.getAccessTokenTtl().toSeconds(),
                jwtService.generateRefreshToken(userDetails.getUsername(), tokenId));
    }

    /**
     * Kiểm tra refresh token: đúng chữ ký, đúng loại, còn hạn, có trong DB và chưa
     * bị thu hồi
     */
    private RefreshToken findValidRefreshToken(String refreshTokenValue) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshTokenValue);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenId(claims.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không tồn tại"));
        if (stored.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã bị thu hồi");
        }
        return stored;
    }
}