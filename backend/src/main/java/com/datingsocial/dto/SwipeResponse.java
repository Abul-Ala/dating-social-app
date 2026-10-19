package com.datingsocial.dto;

public record SwipeResponse(
    boolean isMatch,
    Long matchId,
    Long conversationId,
    String message,
    UserProfileDto matchedProfile
) {
    public static SwipeResponse noMatch() {
        return new SwipeResponse(false, null, null, "Swipe recorded", null);
    }

    public static SwipeResponse matched(Long matchId, Long conversationId, UserProfileDto matchedProfile) {
        return new SwipeResponse(true, matchId, conversationId, "It's a Match!", matchedProfile);
    }
}
