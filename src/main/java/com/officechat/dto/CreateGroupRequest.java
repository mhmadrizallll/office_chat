package com.officechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(

    @NotBlank(message = "Nama grup wajib diisi")
    @Size(max = 150, message = "Nama grup maksimal 150 karakter")
    String groupName,

    @NotEmpty(message = "Minimal satu anggota harus dipilih")
    @Size(max = 100, message = "Maksimal 100 anggota dalam satu grup")
    List<Long> memberIds

) {}