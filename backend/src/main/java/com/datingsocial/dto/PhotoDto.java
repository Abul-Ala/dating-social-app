package com.datingsocial.dto;

public record PhotoDto(
    Long id,
    String photoUrl,
    boolean isPrimary,
    int sortOrder
) {}
