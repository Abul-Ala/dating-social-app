package com.datingsocial.service;

import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.Gender;
import com.datingsocial.model.GenderPreference;
import com.datingsocial.model.Interest;
import com.datingsocial.model.UserProfile;
import com.datingsocial.repository.BlockRepository;
import com.datingsocial.repository.LikeRepository;
import com.datingsocial.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DiscoveryService {

    private final UserProfileRepository userProfileRepository;
    private final LikeRepository likeRepository;
    private final BlockRepository blockRepository;
    private final ProfileService profileService;

    public DiscoveryService(
        UserProfileRepository userProfileRepository,
        LikeRepository likeRepository,
        BlockRepository blockRepository,
        ProfileService profileService
    ) {
        this.userProfileRepository = userProfileRepository;
        this.likeRepository = likeRepository;
        this.blockRepository = blockRepository;
        this.profileService = profileService;
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getDiscoveryFeed(
        Long currentUserId,
        Integer minAge,
        Integer maxAge,
        Gender requestedGender,
        Double maxDistanceKm
    ) {
        UserProfile currentProfile = userProfileRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Collect excluded user IDs (already swiped, blocked, and self)
        Set<Long> excluded = new HashSet<>();
        excluded.add(currentUserId);
        excluded.addAll(likeRepository.findSwipedUserIdsByLikerId(currentUserId));
        excluded.addAll(blockRepository.findAllBlockedUserIdsRelatedTo(currentUserId));

        // Determine gender filter
        Gender targetGender = requestedGender;
        if (targetGender == null && currentProfile.getInterestedIn() != null) {
            if (currentProfile.getInterestedIn() == GenderPreference.MALE) {
                targetGender = Gender.MALE;
            } else if (currentProfile.getInterestedIn() == GenderPreference.FEMALE) {
                targetGender = Gender.FEMALE;
            }
        }

        List<UserProfile> candidates = userProfileRepository.findDiscoveryCandidates(
            currentUserId,
            new ArrayList<>(excluded),
            targetGender
        );

        // If no unswiped candidates with target gender exist, try unswiped with any gender
        if (candidates.isEmpty()) {
            candidates = userProfileRepository.findDiscoveryCandidates(
                currentUserId,
                new ArrayList<>(excluded),
                null
            );
        }

        // If still empty, allow rediscovery (show repeated users, excluding only self and blocked)
        if (candidates.isEmpty()) {
            Set<Long> blockedOrSelf = new HashSet<>();
            blockedOrSelf.add(currentUserId);
            blockedOrSelf.addAll(blockRepository.findAllBlockedUserIdsRelatedTo(currentUserId));
            candidates = userProfileRepository.findDiscoveryCandidates(
                currentUserId,
                new ArrayList<>(blockedOrSelf),
                targetGender
            );
            if (candidates.isEmpty()) {
                candidates = userProfileRepository.findDiscoveryCandidates(
                    currentUserId,
                    new ArrayList<>(blockedOrSelf),
                    null
                );
            }
        }

        int minAgeFilter = minAge != null ? minAge : 18;
        int maxAgeFilter = maxAge != null ? maxAge : 100;
        double maxDist = maxDistanceKm != null ? maxDistanceKm : 500.0;

        List<UserProfileDto> results = new ArrayList<>();

        for (UserProfile candidate : candidates) {
            int candidateAge = candidate.getAge();
            if (candidateAge < minAgeFilter || candidateAge > maxAgeFilter) {
                continue;
            }

            // Calculate distance
            double distance = calculateDistanceKm(
                currentProfile.getLatitude(), currentProfile.getLongitude(),
                candidate.getLatitude(), candidate.getLongitude()
            );

            if (distance > maxDist && candidates.size() > 5) {
                continue;
            }

            int score = calculateCompatibility(currentProfile, candidate, distance);
            results.add(profileService.toDto(candidate, Math.round(distance * 10.0) / 10.0, score));
        }

        // Sort by highest compatibility score first
        results.sort(Comparator.comparingInt(UserProfileDto::compatibilityScore).reversed());
        return results;
    }

    private int calculateCompatibility(UserProfile current, UserProfile candidate, double distanceKm) {
        Set<Interest> setA = current.getInterests() != null ? current.getInterests() : Collections.emptySet();
        Set<Interest> setB = candidate.getInterests() != null ? candidate.getInterests() : Collections.emptySet();

        // Jaccard similarity on interests (0 to 60 points)
        int sharedCount = 0;
        for (Interest a : setA) {
            if (setB.contains(a)) {
                sharedCount++;
            }
        }
        int unionSize = Math.max(1, setA.size() + setB.size() - sharedCount);
        double interestRatio = (double) sharedCount / unionSize;
        int interestPoints = (int) Math.round(interestRatio * 60);

        // Age proximity (0 to 20 points)
        int ageDiff = Math.abs(current.getAge() - candidate.getAge());
        int agePoints = Math.max(0, 20 - (ageDiff * 2));

        // Distance proximity (0 to 20 points)
        int distancePoints = (int) Math.max(0, 20 - (distanceKm / 5.0));

        int total = interestPoints + agePoints + distancePoints;
        // Clamp between 45% and 98% for meaningful user engagement
        return Math.min(98, Math.max(45, total));
    }

    private double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 12.0; // Default reasonable distance
        }
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
