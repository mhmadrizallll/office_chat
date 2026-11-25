package com.officechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(

    @NotBlank(message = "Pesan tidak boleh kosong")
    @Size(max = 5000, message = "Pesan maksimal 5000 karakter")
    String messageContent

) {}