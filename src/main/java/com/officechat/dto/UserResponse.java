
package com.officechat.dto;

public record UserResponse(
    Long userId,
    String employeeId,
    String fullName,
    String email,
    String phone,
    String profileImage,
    Long departmentId,
    String departmentName,
    String role,
    String status,
    Integer isOnline
) {}
