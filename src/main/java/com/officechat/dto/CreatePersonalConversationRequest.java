package com.officechat.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePersonalConversationRequest(

    @NotNull(message = "User ID tujuan wajib diisi")
    Long targetUserId

) {}