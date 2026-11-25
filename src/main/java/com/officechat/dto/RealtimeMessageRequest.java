package com.officechat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RealtimeMessageRequest(

    @NotBlank
    @Size(max = 5000)
    String messageContent

) {}