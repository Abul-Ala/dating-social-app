package com.datingsocial.dto;

import jakarta.validation.constraints.NotNull;

public record SwipeRequest(
    @NotNull(message = "targetUserId is required")
    Long targetUserId,

    @NotNull(message = "isLike is required")
    Boolean isLike
) {}
