package com.datingsocial.dto;

import com.datingsocial.model.Gender;
import com.datingsocial.model.GenderPreference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 3, message = "Password must be at least 3 characters")
    String password,

    @NotBlank(message = "Display name is required")
    @Size(min = 2, max = 50, message = "Display name must be between 2 and 50 characters")
    String displayName,

    @NotNull(message = "Birth date is required")
    LocalDate birthDate,

    @NotNull(message = "Gender is required")
    Gender gender,

    GenderPreference interestedIn,

    String city,
    Double latitude,
    Double longitude,
    String initialPhotoUrl,
    List<Long> interestIds
) {}
