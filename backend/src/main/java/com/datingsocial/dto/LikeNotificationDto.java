package com.datingsocial.dto;

import java.time.LocalDateTime;

public record LikeNotificationDto(
    Long likerUserId,
    String likerName,
    String likerPhotoUrl,
    String message,
    LocalDateTime likedAt,
    boolean isMatched
) {}
