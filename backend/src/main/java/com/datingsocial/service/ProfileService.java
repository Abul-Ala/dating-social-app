package com.datingsocial.service;

import com.datingsocial.dto.*;
import com.datingsocial.model.Interest;
import com.datingsocial.model.UserPhoto;
import com.datingsocial.model.UserProfile;
import com.datingsocial.repository.InterestRepository;
import com.datingsocial.repository.UserPhotoRepository;
import com.datingsocial.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final InterestRepository interestRepository;
    private final UserActivityTracker userActivityTracker;

    public ProfileService(
        UserProfileRepository userProfileRepository,
        UserPhotoRepository userPhotoRepository,
        InterestRepository interestRepository,
        UserActivityTracker userActivityTracker
    ) {
        this.userProfileRepository = userProfileRepository;
        this.userPhotoRepository = userPhotoRepository;
        this.interestRepository = interestRepository;
        this.userActivityTracker = userActivityTracker;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileByUserId(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found for userId: " + userId));
        return toDto(profile, null, 100);
    }

    @Transactional(readOnly = true)
    public List<InterestDto> getAllInterests() {
        return interestRepository.findAll().stream()
            .map(this::toInterestDto)
            .toList();
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found for userId: " + userId));

        if (request.displayName() != null && !request.displayName().isBlank()) {
            profile.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio());
        }
        if (request.occupation() != null) {
            profile.setOccupation(request.occupation());
        }
        if (request.city() != null) {
            profile.setCity(request.city());
        }
        if (request.latitude() != null) {
            profile.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            profile.setLongitude(request.longitude());
        }
        if (request.interestedIn() != null) {
            profile.setInterestedIn(request.interestedIn());
        }
        if (request.hideDistance() != null) {
            profile.setHideDistance(request.hideDistance());
        }
        if (request.hideOnlineStatus() != null) {
            profile.setHideOnlineStatus(request.hideOnlineStatus());
        }
        if (request.interestIds() != null) {
            Set<Interest> interests = new HashSet<>(interestRepository.findAllById(request.interestIds()));
            profile.setInterests(interests);
        }

        profile.setUpdatedAt(LocalDateTime.now());
        profile = userProfileRepository.save(profile);
        return toDto(profile, null, 100);
    }

    @Transactional
    public UserProfileDto addPhoto(Long userId, String photoUrl, boolean isPrimary) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (isPrimary) {
            profile.getPhotos().forEach(p -> p.setPrimary(false));
        }

        UserPhoto photo = new UserPhoto(photoUrl, isPrimary, profile.getPhotos().size());
        profile.addPhoto(photo);
        profile = userProfileRepository.save(profile);
        return toDto(profile, null, 100);
    }

    @Transactional
    public void deletePhoto(Long userId, Long photoId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        UserPhoto photo = userPhotoRepository.findById(photoId)
            .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (photo.getProfile().getId().equals(profile.getId())) {
            profile.removePhoto(photo);
            userProfileRepository.save(profile);
        }
    }

    public UserProfileDto toDto(UserProfile profile, Double distanceKm, int compatibilityScore) {
        List<PhotoDto> photos = profile.getPhotos().stream()
            .map(this::toPhotoDto)
            .toList();

        List<InterestDto> interests = profile.getInterests().stream()
            .map(this::toInterestDto)
            .toList();

        Double effectiveDistance = profile.isHideDistance() ? null : distanceKm;
        Long userId = profile.getUser() != null ? profile.getUser().getId() : null;
        String status = userActivityTracker.getStatusDescription(userId, profile.isHideOnlineStatus());

        return new UserProfileDto(
            profile.getId(),
            userId,
            profile.getDisplayName(),
            profile.getAge(),
            profile.getGender(),
            profile.getInterestedIn(),
            profile.getBio(),
            profile.getOccupation(),
            profile.getCity(),
            profile.isVerified(),
            profile.isHideDistance(),
            profile.isHideOnlineStatus(),
            photos,
            interests,
            effectiveDistance,
            compatibilityScore,
            status
        );
    }

    public InterestDto toInterestDto(Interest interest) {
        return new InterestDto(
            interest.getId(),
            interest.getName(),
            interest.getCategory(),
            interest.getIcon()
        );
    }

    public PhotoDto toPhotoDto(UserPhoto photo) {
        return new PhotoDto(
            photo.getId(),
            photo.getPhotoUrl(),
            photo.isPrimary(),
            photo.getSortOrder()
        );
    }
}
