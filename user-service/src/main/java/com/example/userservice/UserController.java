package com.example.userservice;

import com.example.userservice.security.AuthUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Tên container đang chạy code này (user-service-1 hoặc user-service-2)
    private String instanceName() {
        return System.getenv().getOrDefault("HOSTNAME", "unknown");
    }

    /**
     * GET /api/users/whoami - ai đã đăng nhập cũng gọi được.
     * Cho biết: instance nào xử lý (load balancing) + token của ai, có quyền gì
     * (phân quyền).
     */
    @GetMapping("/api/users/whoami")
    public Map<String, Object> whoami(Authentication authentication) {
        AuthUser currentUser = (AuthUser) authentication.getPrincipal();
        System.out.println("[" + instanceName() + "] handled GET /api/users/whoami by " + currentUser.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "user-service");
        result.put("instance", instanceName());
        result.put("userId", currentUser.getId());
        result.put("username", currentUser.getUsername());
        result.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList());
        return result;
    }

    /**
     * GET /api/users - danh sách nhân sự kèm tên phòng ban.
     * PHÂN QUYỀN: chỉ ai có quyền USER_VIEW_LIST (ADMIN, MANAGER).
     * Quyền đọc từ access token -> user-service KHÔNG cần truy cập DB của
     * auth-service.
     */
    @GetMapping("/api/users")
    @PreAuthorize("hasAuthority('USER_VIEW_LIST')")
    public List<UserResponse> getAllUsers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        System.out.println("[" + instanceName() + "] handled GET /api/users");

        // 1. Lấy user từ DB của user-service
        List<User> users = userRepository.findAll();

        // 2. Gọi department-service (theo TÊN service, không dùng IP/port)
        Map<Long, String> departmentNames = new HashMap<>();
        try {
            // TOKEN RELAY: chuyển tiếp NGUYÊN header Authorization của người dùng sang
            // department-service,
            // để department-service cũng biết "ai đang gọi" và tự kiểm tra quyền nếu cần
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, authorization);

            DepartmentDto[] departments = restTemplate.exchange(
                    "http://department-service/api/departments",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    DepartmentDto[].class).getBody();
            if (departments != null) {
                for (DepartmentDto d : departments) {
                    departmentNames.put(d.getId(), d.getDepartmentName());
                }
            }
        } catch (Exception e) {
            // department-service lỗi/tắt/từ chối -> vẫn trả user, department_name = null
            System.out.println("Cannot call department-service: " + e.getMessage());
        }

        // 3. Ghép department_name vào từng user theo department_id
        return users.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getUserName(),
                        departmentNames.get(u.getDepartmentId())))
                .toList();
    }
}