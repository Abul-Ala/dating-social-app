package com.datingsocial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
    @NotNull(message = "targetUserId is required")
    Long targetUserId,

    @NotBlank(message = "Reason is required")
    String reason,

    String details
) {}
