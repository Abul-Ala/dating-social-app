package com.datingsocial.dto;

import com.datingsocial.model.MessageType;

import java.time.LocalDateTime;

public record MessageDto(
    Long id,
    Long conversationId,
    Long senderId,
    String senderName,
    String content,
    MessageType type,
    boolean isRead,
    LocalDateTime createdAt
) {}
