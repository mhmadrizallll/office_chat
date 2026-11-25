
package com.officechat.service;

import com.officechat.dto.CreateUserRequest;
import com.officechat.dto.UpdateUserRequest;
import com.officechat.dto.UserResponse;
import com.officechat.entity.Department;
import com.officechat.entity.User;
import com.officechat.repository.DepartmentRepository;
import com.officechat.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Karyawan tidak ditemukan"
                ));

        return toResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email sudah digunakan"
            );
        }

        if (userRepository.existsByEmployeeId(request.employeeId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee ID sudah digunakan"
            );
        }

        String role = request.role() == null
                ? "EMPLOYEE"
                : request.role().toUpperCase();

        if (!role.equals("ADMIN") && !role.equals("EMPLOYEE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role tidak valid"
            );
        }

        User user = new User();

        user.setEmployeeId(request.employeeId());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setPhone(request.phone());
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setIsOnline(0);

        if (request.departmentId() != null) {
            Department department = departmentRepository
                    .findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Departemen tidak ditemukan"
                    ));

            user.setDepartment(department);
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Karyawan tidak ditemukan"
                ));

        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getUserId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Email sudah digunakan"
                    );
                });

        String role = request.role() == null
                ? user.getRole()
                : request.role().toUpperCase();

        String status = request.status() == null
                ? user.getStatus()
                : request.status().toUpperCase();

        if (!role.equals("ADMIN") && !role.equals("EMPLOYEE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role tidak valid"
            );
        }

        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status tidak valid"
            );
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(role);
        user.setStatus(status);

        if (request.departmentId() != null) {
            Department department = departmentRepository
                    .findById(request.departmentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Departemen tidak ditemukan"
                    ));

            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {

        Department department = user.getDepartment();

        return new UserResponse(
                user.getUserId(),
                user.getEmployeeId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImage(),
                department != null ? department.getDepartmentId() : null,
                department != null ? department.getDepartmentName() : null,
                user.getRole(),
                user.getStatus(),
                user.getIsOnline()
        );
    }
}
