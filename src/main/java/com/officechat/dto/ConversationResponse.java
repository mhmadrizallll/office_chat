package com.officechat.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
    Long conversationId,
    String conversationType,
    String conversationName,
    String groupImage,
    LocalDateTime createdAt
) {}