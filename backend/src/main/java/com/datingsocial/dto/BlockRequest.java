package com.datingsocial.dto;

import jakarta.validation.constraints.NotNull;

public record BlockRequest(
    @NotNull(message = "targetUserId is required")
    Long targetUserId,

    String reason
) {}
