package com.datingsocial.dto;

public record VideoCallSignalDto(
    String callId,
    String action, // "ACCEPT", "REJECT", "END"
    String reason
) {}
