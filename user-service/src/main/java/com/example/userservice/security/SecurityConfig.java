package com.example.userservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // bật @PreAuthorize trên các hàm của controller
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityErrorHandler securityErrorHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, SecurityErrorHandler securityErrorHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API dùng token, không dùng cookie/session -> không cần CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: không lưu session -> instance nào cũng xử lý được (load balancing
                // thoải mái)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Prometheus kéo metric + kiểm tra sức khỏe: không cần token.
                        // An toàn vì Gateway KHÔNG route /actuator và user-service không mở port ra
                        // ngoài.
                        .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                        // Mọi API còn lại: bắt buộc có access token hợp lệ.
                        // Quyền chi tiết khai báo bằng @PreAuthorize ở controller.
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler) // 401
                        .accessDeniedHandler(securityErrorHandler)) // 403
                // Chạy filter JWT trước filter đăng nhập mặc định của Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}