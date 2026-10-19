package com.datingsocial.dto;

public record VoiceSignalDto(
    String type,        // "offer", "answer", "candidate", "bye"
    Long senderId,
    Long targetId,
    String roomId,
    String payload      // SDP or ICE candidate JSON
) {}
