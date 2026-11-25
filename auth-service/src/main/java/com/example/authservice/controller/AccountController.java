package com.example.authservice.controller;

import com.example.authservice.dto.UserCreateRequest;
import com.example.authservice.dto.UserResponse;
import com.example.authservice.dto.UserUpdateRequest;
import com.example.authservice.security.CustomUserDetails;
import com.example.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Các API quản lý tài khoản. Tất cả đều cần access token.
 * Đường dẫn /api/accounts (không phải /api/users) vì /api/users/** đã được
 * Gateway route sang user-service.
 * 
 * @PreAuthorize được kiểm tra TRƯỚC khi vào thân hàm; không thỏa -> 403
 *               Forbidden.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    /** POST /api/accounts - tạo tài khoản. Chỉ ai có quyền USER_CREATE (ADMIN). */
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    /**
     * GET /api/accounts - xem danh sách. Ai có quyền USER_VIEW_LIST (ADMIN,
     * MANAGER).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW_LIST')")
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    /**
     * GET /api/accounts/{id} - xem 1 tài khoản. Được phép khi:
     * - có quyền USER_VIEW_DETAIL (ADMIN: xem ai cũng được), HOẶC
     * - có quyền USER_VIEW_SELF và id trên URL là id của chính mình (EMPLOYEE).
     * Trong biểu thức: #id là tham số id của hàm, authentication.principal là
     * CustomUserDetails.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW_DETAIL') "
            + "or (hasAuthority('USER_VIEW_SELF') and #id == authentication.principal.id)")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * PUT /api/accounts/{id} - cập nhật. ADMIN sửa ai cũng được, EMPLOYEE chỉ sửa
     * chính mình.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE') "
            + "or (hasAuthority('USER_UPDATE_SELF') and #id == authentication.principal.id)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /**
     * DELETE /api/accounts/{id} - xóa tài khoản. Chỉ ai có quyền USER_DELETE
     * (ADMIN).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}