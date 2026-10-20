package com.datingsocial.service;

import com.datingsocial.dto.StoryDto;
import com.datingsocial.dto.UserStoryGroupDto;
import com.datingsocial.model.Story;
import com.datingsocial.model.User;
import com.datingsocial.model.UserPhoto;
import com.datingsocial.repository.BlockRepository;
import com.datingsocial.repository.StoryRepository;
import com.datingsocial.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoryService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final AppEventService appEventService;

    public StoryService(
        StoryRepository storyRepository,
        UserRepository userRepository,
        BlockRepository blockRepository,
        AppEventService appEventService
    ) {
        this.storyRepository = storyRepository;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.appEventService = appEventService;
    }

    public StoryDto createStory(Long userId, MultipartFile file, String caption) throws IOException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Story media file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "story_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path uploadsDir = Paths.get("uploads");
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        Path targetPath = uploadsDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String mediaUrl = "/uploads/" + filename;
        Story story = new Story(user, mediaUrl, caption);
        Story saved = storyRepository.save(story);

        StoryDto dto = toDto(saved);
        try {
            appEventService.broadcastEvent("NEW_STORY", dto);
        } catch (Exception e) {
            // ignore
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<UserStoryGroupDto> getStoriesTray(Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> blockedIds = blockRepository.findAllBlockedUserIdsRelatedTo(currentUserId);

        List<Story> activeStories;
        if (blockedIds == null || blockedIds.isEmpty()) {
            activeStories = storyRepository.findByExpiresAtAfterOrderByCreatedAtDesc(now);
        } else {
            activeStories = storyRepository.findActiveStoriesExcludingUsers(now, blockedIds);
        }

        // Group stories by User preserving insertion order
        Map<Long, List<Story>> grouped = new LinkedHashMap<>();
        for (Story s : activeStories) {
            grouped.computeIfAbsent(s.getUser().getId(), k -> new ArrayList<>()).add(s);
        }

        List<UserStoryGroupDto> result = new ArrayList<>();

        // If current user has stories, put them first
        if (grouped.containsKey(currentUserId)) {
            List<Story> myStories = grouped.remove(currentUserId);
            User me = myStories.get(0).getUser();
            result.add(new UserStoryGroupDto(
                me.getId(),
                getUserDisplayName(me),
                getUserAvatarUrl(me),
                true,
                myStories.stream().map(this::toDto).collect(Collectors.toList())
            ));
        }

        // Add remaining users' stories
        for (Map.Entry<Long, List<Story>> entry : grouped.entrySet()) {
            List<Story> userStories = entry.getValue();
            User user = userStories.get(0).getUser();
            result.add(new UserStoryGroupDto(
                user.getId(),
                getUserDisplayName(user),
                getUserAvatarUrl(user),
                false,
                userStories.stream().map(this::toDto).collect(Collectors.toList())
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<StoryDto> getMyStories(Long currentUserId) {
        LocalDateTime now = LocalDateTime.now();
        return storyRepository.findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(currentUserId, now)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public void deleteStory(Long currentUserId, Long storyId) {
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new IllegalArgumentException("Story not found: " + storyId));

        if (!story.getUser().getId().equals(currentUserId)) {
            throw new IllegalStateException("You can only delete your own stories");
        }

        storyRepository.delete(story);
    }

    public void incrementViewCount(Long storyId) {
        storyRepository.findById(storyId).ifPresent(story -> {
            story.setViewCount(story.getViewCount() + 1);
            storyRepository.save(story);
        });
    }

    private StoryDto toDto(Story story) {
        User user = story.getUser();
        return new StoryDto(
            story.getId(),
            user.getId(),
            getUserDisplayName(user),
            getUserAvatarUrl(user),
            story.getMediaUrl(),
            story.getCaption(),
            story.getCreatedAt(),
            story.getExpiresAt(),
            story.getTimeAgo(),
            story.getViewCount()
        );
    }

    private String getUserDisplayName(User user) {
        if (user.getProfile() != null && user.getProfile().getDisplayName() != null && !user.getProfile().getDisplayName().isBlank()) {
            return user.getProfile().getDisplayName();
        }
        return "User";
    }

    private String getUserAvatarUrl(User user) {
        if (user.getProfile() != null && user.getProfile().getPhotos() != null && !user.getProfile().getPhotos().isEmpty()) {
            return user.getProfile().getPhotos().stream()
                .filter(UserPhoto::isPrimary)
                .map(UserPhoto::getPhotoUrl)
                .findFirst()
                .orElse(user.getProfile().getPhotos().get(0).getPhotoUrl());
        }
        return user.getProfile() != null && user.getProfile().getGender() != null && user.getProfile().getGender().name().equals("MALE")
            ? "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800"
            : "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800";
    }
}
