package com.datingsocial.dto;

import java.time.LocalDateTime;

public record MatchDto(
    Long matchId,
    Long conversationId,
    UserProfileDto matchedUser,
    LocalDateTime matchedAt
) {}
