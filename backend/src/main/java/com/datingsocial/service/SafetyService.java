package com.datingsocial.service;

import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.*;
import com.datingsocial.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SafetyService {

    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileService profileService;

    public SafetyService(
        BlockRepository blockRepository,
        ReportRepository reportRepository,
        MatchRepository matchRepository,
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        ProfileService profileService
    ) {
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.profileService = profileService;
    }

    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId, String reason) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        User blocker = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        User blocked = userRepository.findById(targetUserId)
            .orElseThrow(() -> new RuntimeException("Target user not found"));

        // Deactivate active match if present
        matchRepository.findActiveMatchBetween(currentUserId, targetUserId).ifPresent(m -> {
            m.setActive(false);
            matchRepository.save(m);
        });

        if (!blockRepository.existsByBlockerIdAndBlockedId(currentUserId, targetUserId)) {
            Block block = new Block(blocker, blocked, reason != null ? reason : "User initiated block");
            blockRepository.save(block);
        }
    }

    @Transactional
    public void unmatchUser(Long currentUserId, Long targetUserId) {
        matchRepository.findActiveMatchBetween(currentUserId, targetUserId).ifPresent(m -> {
            m.setActive(false);
            matchRepository.save(m);
        });
    }

    @Transactional
    public void reportUser(Long currentUserId, Long targetUserId, String reason, String details) {
        User reporter = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Reporter not found"));
        User reported = userRepository.findById(targetUserId)
            .orElseThrow(() -> new RuntimeException("Reported user not found"));

        Report report = new Report(reporter, reported, reason, details);
        reportRepository.save(report);

        // Automatically block reported user for safety
        blockUser(currentUserId, targetUserId, "Auto-blocked due to report: " + reason);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getBlockedUsers(Long currentUserId) {
        List<Block> blocks = blockRepository.findByBlockerId(currentUserId);
        return blocks.stream().map(b -> {
            UserProfile p = userProfileRepository.findByUserId(b.getBlocked().getId()).orElse(null);
            return p != null ? profileService.toDto(p, null, 0) : null;
        }).filter(java.util.Objects::nonNull).toList();
    }
}
