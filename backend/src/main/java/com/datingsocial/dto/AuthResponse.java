package com.datingsocial.dto;

import com.datingsocial.model.Role;

public record AuthResponse(
    String token,
    Long userId,
    String email,
    Role role,
    UserProfileDto profile
) {}
