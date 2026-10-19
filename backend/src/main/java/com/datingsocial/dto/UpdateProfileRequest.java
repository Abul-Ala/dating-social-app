package com.datingsocial.dto;

import com.datingsocial.model.GenderPreference;

import java.util.List;

public record UpdateProfileRequest(
    String displayName,
    String bio,
    String occupation,
    String city,
    Double latitude,
    Double longitude,
    GenderPreference interestedIn,
    List<Long> interestIds,
    Boolean hideDistance,
    Boolean hideOnlineStatus
) {}
