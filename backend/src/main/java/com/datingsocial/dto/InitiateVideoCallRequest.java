package com.datingsocial.dto;

import jakarta.validation.constraints.NotNull;

public record InitiateVideoCallRequest(
    @NotNull Long targetUserId,
    Long conversationId
) {}
