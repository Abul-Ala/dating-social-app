package com.datingsocial.dto;

import com.datingsocial.model.Gender;
import com.datingsocial.model.GenderPreference;

import java.util.List;

public record UserProfileDto(
    Long id,
    Long userId,
    String displayName,
    int age,
    Gender gender,
    GenderPreference interestedIn,
    String bio,
    String occupation,
    String city,
    boolean isVerified,
    boolean hideDistance,
    boolean hideOnlineStatus,
    List<PhotoDto> photos,
    List<InterestDto> interests,
    Double distanceKm,
    int compatibilityScore,
    String userStatus
) {
    public UserProfileDto(
        Long id,
        Long userId,
        String displayName,
        int age,
        Gender gender,
        GenderPreference interestedIn,
        String bio,
        String occupation,
        String city,
        boolean isVerified,
        boolean hideDistance,
        boolean hideOnlineStatus,
        List<PhotoDto> photos,
        List<InterestDto> interests,
        Double distanceKm,
        int compatibilityScore
    ) {
        this(
            id,
            userId,
            displayName,
            age,
            gender,
            interestedIn,
            bio,
            occupation,
            city,
            isVerified,
            hideDistance,
            hideOnlineStatus,
            photos,
            interests,
            distanceKm,
            compatibilityScore,
            "Offline"
        );
    }
}
