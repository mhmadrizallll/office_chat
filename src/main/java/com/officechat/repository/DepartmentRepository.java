package com.officechat.repository;

import com.officechat.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository
        extends JpaRepository<Department, Long> {
        boolean existsByDepartmentName(String departmentName);
}