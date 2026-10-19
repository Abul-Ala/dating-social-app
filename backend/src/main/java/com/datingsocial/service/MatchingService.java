package com.datingsocial.service;

import com.datingsocial.dto.MatchDto;
import com.datingsocial.dto.LikeNotificationDto;
import com.datingsocial.dto.PhotoDto;
import com.datingsocial.dto.SwipeResponse;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.*;
import com.datingsocial.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MatchingService {

    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfileService profileService;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlockRepository blockRepository;

    public MatchingService(
        LikeRepository likeRepository,
        MatchRepository matchRepository,
        ConversationRepository conversationRepository,
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        ProfileService profileService,
        SimpMessagingTemplate messagingTemplate,
        BlockRepository blockRepository
    ) {
        this.likeRepository = likeRepository;
        this.matchRepository = matchRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.profileService = profileService;
        this.messagingTemplate = messagingTemplate;
        this.blockRepository = blockRepository;
    }

    @Transactional
    public SwipeResponse recordSwipe(Long currentUserId, Long targetUserId, boolean isLike) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Current user not found"));
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new RuntimeException("Target user not found"));

        // Record or update the swipe
        Like like = likeRepository.findByLikerIdAndLikedId(currentUserId, targetUserId)
            .orElse(new Like(currentUser, targetUser, isLike));
        like.setLike(isLike);
        likeRepository.save(like);

        if (!isLike) {
            return SwipeResponse.noMatch();
        }

        // Check if target user has previously liked current user
        boolean hasMutualLike = likeRepository.existsByLikerIdAndLikedIdAndIsLikeTrue(targetUserId, currentUserId);

        // Notify target user that someone liked their profile
        try {
            UserProfile currentProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
            UserProfileDto currentDto = currentProfile != null ? profileService.toDto(currentProfile, null, 90) : null;
            String photoUrl = null;
            if (currentDto != null && currentDto.photos() != null && !currentDto.photos().isEmpty()) {
                photoUrl = currentDto.photos().stream()
                    .filter(PhotoDto::isPrimary)
                    .findFirst()
                    .map(PhotoDto::photoUrl)
                    .orElse(currentDto.photos().get(0).photoUrl());
            }
            String name = currentDto != null && currentDto.displayName() != null ? currentDto.displayName() : "Someone";
            LikeNotificationDto notifyDto = new LikeNotificationDto(
                currentUserId,
                name,
                photoUrl,
                name + " liked your profile! 💕",
                like.getCreatedAt(),
                hasMutualLike
            );
            messagingTemplate.convertAndSend("/topic/user/" + targetUserId + "/likes", notifyDto);
        } catch (Exception e) {
            // Ignore WebSocket delivery failure in background
        }

        if (hasMutualLike) {
            // Check if active match already exists
            Optional<Match> existingMatch = matchRepository.findActiveMatchBetween(currentUserId, targetUserId);
            Match match;
            Conversation conversation;

            if (existingMatch.isPresent()) {
                match = existingMatch.get();
                Conversation existingConvo = match.getConversation();
                if (existingConvo == null) {
                    final Match finalMatch = match;
                    conversation = conversationRepository.findByMatchId(match.getId())
                        .orElseGet(() -> conversationRepository.save(new Conversation(finalMatch)));
                } else {
                    conversation = existingConvo;
                }
            } else {
                match = matchRepository.save(new Match(currentUser, targetUser));
                conversation = conversationRepository.save(new Conversation(match));
                match.setConversation(conversation);
            }

            UserProfile targetProfile = userProfileRepository.findByUserId(targetUserId)
                .orElse(null);
            UserProfileDto targetDto = targetProfile != null ? profileService.toDto(targetProfile, null, 95) : null;

            // Notify target user in real-time via WebSocket
            try {
                UserProfile currentProfile = userProfileRepository.findByUserId(currentUserId).orElse(null);
                UserProfileDto currentDto = currentProfile != null ? profileService.toDto(currentProfile, null, 95) : null;
                MatchDto notifyDto = new MatchDto(match.getId(), conversation.getId(), currentDto, match.getMatchedAt());
                messagingTemplate.convertAndSend("/topic/user/" + targetUserId + "/matches", notifyDto);
            } catch (Exception e) {
                // Ignore WebSocket delivery failure in background
            }

            return SwipeResponse.matched(match.getId(), conversation.getId(), targetDto);
        }

        return SwipeResponse.noMatch();
    }

    @Transactional
    public List<MatchDto> getUserMatches(Long userId) {
        List<Match> matches = matchRepository.findAllActiveMatchesForUser(userId);
        return matches.stream().filter(m -> {
            User other = m.getOtherUser(userId);
            return !blockRepository.existsByBlockerIdAndBlockedId(userId, other.getId()) &&
                   !blockRepository.existsByBlockerIdAndBlockedId(other.getId(), userId);
        }).map(m -> {
            User other = m.getOtherUser(userId);
            UserProfile profile = userProfileRepository.findByUserId(other.getId()).orElse(null);
            UserProfileDto otherDto = profile != null ? profileService.toDto(profile, null, 90) : null;
            Long convoId = conversationRepository.findByMatchId(m.getId())
                .orElseGet(() -> conversationRepository.save(new Conversation(m)))
                .getId();
            return new MatchDto(m.getId(), convoId, otherDto, m.getMatchedAt());
        }).toList();
    }


    @Transactional(readOnly = true)
    public List<LikeNotificationDto> getLikesReceived(Long userId) {
        List<Like> likes = likeRepository.findLikesReceivedByUserId(userId);
        return likes.stream().filter(l -> {
            Long likerId = l.getLiker().getId();
            return !blockRepository.existsByBlockerIdAndBlockedId(userId, likerId) &&
                   !blockRepository.existsByBlockerIdAndBlockedId(likerId, userId) &&
                   matchRepository.findActiveMatchBetween(userId, likerId).isEmpty();
        }).map(l -> {
            Long likerId = l.getLiker().getId();
            UserProfile profile = userProfileRepository.findByUserId(likerId).orElse(null);
            UserProfileDto dto = profile != null ? profileService.toDto(profile, null, 90) : null;
            String photoUrl = null;
            if (dto != null && dto.photos() != null && !dto.photos().isEmpty()) {
                photoUrl = dto.photos().stream()
                    .filter(PhotoDto::isPrimary)
                    .findFirst()
                    .map(PhotoDto::photoUrl)
                    .orElse(dto.photos().get(0).photoUrl());
            }
            String name = dto != null && dto.displayName() != null ? dto.displayName() : "Someone";
            boolean isMatched = matchRepository.findActiveMatchBetween(userId, likerId).isPresent();
            return new LikeNotificationDto(
                likerId,
                name,
                photoUrl,
                name + " liked your profile! 💕",
                l.getCreatedAt(),
                isMatched
            );
        }).toList();
    }
}
