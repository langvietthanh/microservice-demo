package com.example.authservice.security;

import com.example.authservice.entity.Permission;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * "Người dùng đang đăng nhập" theo cách hiểu của Spring Security.
 * Spring Security không biết entity User của mình, nó chỉ làm việc với
 * UserDetails
 * -> lớp này chuyển User thành UserDetails.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id; // giữ lại id để kiểm tra "có phải chính mình không"
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities = new HashSet<>();

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();

        for (Role role : user.getRoles()) {
            // Role được thêm tiền tố ROLE_ -> dùng với hasRole('ADMIN')
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            // Permission giữ nguyên mã -> dùng với hasAuthority('USER_CREATE')
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
            }
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}