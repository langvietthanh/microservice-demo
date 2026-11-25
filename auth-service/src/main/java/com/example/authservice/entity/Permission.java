package com.example.authservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Quyền: một hành động cụ thể, ví dụ USER_CREATE. Ánh xạ với bảng
 * tbl_permission.
 */
@Entity
@Table(name = "tbl_permission")
@Getter
@Setter
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã quyền, dùng trong code: hasAuthority('USER_CREATE') */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;
}