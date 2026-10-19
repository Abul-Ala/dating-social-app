package com.datingsocial.dto;

import java.util.List;

public record UserStoryGroupDto(
    Long userId,
    String userName,
    String userAvatarUrl,
    boolean isMe,
    List<StoryDto> stories
) {}
