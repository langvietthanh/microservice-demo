package com.example.authservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // bật @PreAuthorize trên các hàm của controller
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API dùng token, không dùng cookie/session -> không cần CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Không tạo session: mỗi request phải tự mang token (stateless -> chạy nhiều
                // instance được)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // login / refresh / logout: không cần access token
                        .requestMatchers("/api/auth/**").permitAll()
                        // Prometheus và health check, vì Gateway không route /actuator
                        // và auth-service không mở port ra ngoài -> chỉ gọi được từ bên trong mạng
                        // Docker
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // Mọi API còn lại: bắt buộc đã đăng nhập.
                        // Quyền chi tiết của từng API được khai báo bằng @PreAuthorize ở controller.
                        .anyRequest().authenticated())
                // Chưa đăng nhập -> trả 401 dạng JSON
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                // Chạy filter JWT trước filter đăng nhập mặc định của Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Thuật toán mã hóa mật khẩu. BCrypt tự sinh salt, mỗi lần mã hóa cho ra một
     * chuỗi khác nhau.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager: dùng trong AuthService để kiểm tra username/password
     * khi đăng nhập
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}