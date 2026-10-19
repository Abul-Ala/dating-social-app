package com.datingsocial.dto;

public record VoiceMatchStatusDto(
    String status,              // "IDLE", "WAITING", "MATCHED", "ENDED"
    String roomId,
    UserProfileDto peer,
    String audioMode,           // "LIVEKIT" or "WEBRTC_P2P"
    String livekitUrl,
    String livekitToken,
    boolean isInitiator,        // true for caller (creates offer), false for receiver
    boolean peerSentMatchRequest,
    boolean isMutualMatch,
    Long conversationId,
    Long createdAt,
    long durationSeconds
) {
    public static VoiceMatchStatusDto idle() {
        return new VoiceMatchStatusDto("IDLE", null, null, "WEBRTC_P2P", null, null, false, false, false, null, null, 60);
    }

    public static VoiceMatchStatusDto waiting(int queueSize) {
        return new VoiceMatchStatusDto("WAITING", null, null, "WEBRTC_P2P", null, null, false, false, false, null, System.currentTimeMillis(), 60);
    }
}
