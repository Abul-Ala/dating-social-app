package com.datingsocial.dto;

import com.datingsocial.model.MessageType;
import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest(
    @NotBlank(message = "Message content is required")
    String content,

    MessageType type
) {}
