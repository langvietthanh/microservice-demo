package com.example.departmentservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    // API lấy danh sách department -> [{id, department_name}]
    @GetMapping("/api/departments")
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
}