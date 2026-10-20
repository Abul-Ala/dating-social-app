package com.datingsocial.dto;

public record VideoCallDto(
    String callId,
    String roomId,
    Long callerId,
    UserProfileDto callerProfile,
    Long targetId,
    UserProfileDto targetProfile,
    Long conversationId,
    String status, // "RINGING", "ACCEPTED", "REJECTED", "ENDED", "BUSY", "MISSED"
    String livekitUrl,
    String token,
    boolean isTargetOnline,
    long createdAt
) {}
