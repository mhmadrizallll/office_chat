package com.officechat.dto;

import java.time.LocalDateTime;

public record MessageResponse(
    Long messageId,
    Long conversationId,
    Long senderId,
    String senderName,
    String messageType,
    String messageContent,
    LocalDateTime sentAt,
    LocalDateTime editedAt
) {}