package com.officechat.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(

    @NotBlank(message = "Employee ID atau email wajib diisi")
    String identifier,

    @NotBlank(message = "Password wajib diisi")
    String password

) {}