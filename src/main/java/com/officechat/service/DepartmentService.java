
package com.officechat.service;

import com.officechat.dto.DepartmentResponse;
import com.officechat.repository.DepartmentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {

        return departmentRepository.findAll()
                .stream()
                .map(department -> new DepartmentResponse(
                        department.getDepartmentId(),
                        department.getDepartmentName(),
                        department.getDescription()
                ))
                .toList();
    }
}
