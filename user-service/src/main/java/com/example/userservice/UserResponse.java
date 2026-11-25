package com.example.userservice;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserResponse {

    private Long id;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("department_name")
    private String departmentName;

    public UserResponse(Long id, String userName, String departmentName) {
        this.id = id;
        this.userName = userName;
        this.departmentName = departmentName;
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}