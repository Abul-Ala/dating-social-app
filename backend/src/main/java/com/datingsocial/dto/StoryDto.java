package com.datingsocial.dto;

import java.time.LocalDateTime;

public record StoryDto(
    Long id,
    Long userId,
    String userName,
    String userAvatarUrl,
    String mediaUrl,
    String caption,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    String timeAgo,
    int viewCount
) {}
