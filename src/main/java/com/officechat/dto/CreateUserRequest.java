
package com.officechat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

    @NotBlank(message = "Employee ID wajib diisi")
    @Size(max = 30)
    String employeeId,

    @NotBlank(message = "Nama lengkap wajib diisi")
    @Size(max = 150)
    String fullName,

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    @Size(max = 150)
    String email,

    @NotBlank(message = "Password wajib diisi")
    @Size(min = 8, max = 72)
    String password,

    @Size(max = 30)
    String phone,

    Long departmentId,

    String role
) {}
