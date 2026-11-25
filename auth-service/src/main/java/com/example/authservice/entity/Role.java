package com.example.authservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Vai trò: một nhóm quyền (ADMIN, MANAGER, EMPLOYEE). Ánh xạ với bảng tbl_role.
 */
@Entity
@Table(name = "tbl_role")
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    /**
     * Một role có nhiều permission, một permission thuộc nhiều role
     * -> quan hệ nhiều-nhiều thông qua bảng nối tbl_role_permission.
     * EAGER: lấy role là lấy luôn danh sách quyền (đơn giản cho bài thực hành).
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tbl_role_permission", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
}