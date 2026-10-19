package com.datingsocial.dto;

import java.time.LocalDateTime;

public record ConversationDto(
    Long id,
    Long matchId,
    UserProfileDto otherUser,
    String lastMessage,
    LocalDateTime lastMessageTime,
    long unreadCount,
    LocalDateTime updatedAt
) {}
