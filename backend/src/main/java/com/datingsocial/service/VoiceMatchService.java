package com.datingsocial.service;

import com.datingsocial.dto.SwipeResponse;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.dto.VoiceMatchStatusDto;
import com.datingsocial.dto.VoiceSignalDto;
import com.datingsocial.model.User;
import com.datingsocial.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class VoiceMatchService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceMatchService.class);

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final MatchingService matchingService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.livekit.url:}")
    private String livekitUrl;

    @Value("${app.livekit.api-key:}")
    private String livekitApiKey;

    @Value("${app.livekit.api-secret:}")
    private String livekitApiSecret;

    // Concurrent waiting queue of User IDs
    private final ConcurrentLinkedQueue<Long> waitingQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Long> waitingTimestamps = new ConcurrentHashMap<>();

    // Active voice rooms: roomId -> Session
    private final ConcurrentHashMap<String, VoiceRoomSession> activeRooms = new ConcurrentHashMap<>();
    // User ID -> roomId
    private final ConcurrentHashMap<Long, String> userToRoom = new ConcurrentHashMap<>();
    // User ID -> SseEmitter for real-time Server-Sent Events
    private final ConcurrentHashMap<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public static class VoiceRoomSession {
        public String roomId;
        public Long user1Id;
        public Long user2Id;
        public boolean user1RequestedMatch = false;
        public boolean user2RequestedMatch = false;
        public boolean isMutualMatch = false;
        public Long conversationId = null;
        public long createdAt = System.currentTimeMillis();

        public VoiceRoomSession(String roomId, Long user1Id, Long user2Id) {
            this.roomId = roomId;
            this.user1Id = user1Id;
            this.user2Id = user2Id;
        }

        public Long getOtherUser(Long currentUserId) {
            return currentUserId.equals(user1Id) ? user2Id : user1Id;
        }

        public boolean hasRequestedMatch(Long currentUserId) {
            return currentUserId.equals(user1Id) ? user1RequestedMatch : user2RequestedMatch;
        }

        public boolean peerRequestedMatch(Long currentUserId) {
            return currentUserId.equals(user1Id) ? user2RequestedMatch : user1RequestedMatch;
        }
    }

    public VoiceMatchService(
        UserRepository userRepository,
        ProfileService profileService,
        MatchingService matchingService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.matchingService = matchingService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Join the voice match waiting pool. If a peer is available, pair them immediately!
     */
    public synchronized VoiceMatchStatusDto joinQueue(Long currentUserId) {
        // If user is already in an active room, return it
        String existingRoomId = userToRoom.get(currentUserId);
        if (existingRoomId != null) {
            VoiceRoomSession session = activeRooms.get(existingRoomId);
            if (session != null && !isSessionExpired(session)) {
                return buildStatusForUser(session, currentUserId);
            } else {
                leaveRoom(currentUserId);
            }
        }

        // Clean stale waiting users (> 90 seconds inactive)
        long now = System.currentTimeMillis();
        waitingTimestamps.entrySet().removeIf(entry -> (now - entry.getValue()) > 90000);

        // Remove current user from waiting queue if already there
        waitingQueue.remove(currentUserId);

        // Search for an available peer in the queue
        Long peerId = null;
        while (!waitingQueue.isEmpty()) {
            Long candidate = waitingQueue.poll();
            if (candidate != null && !candidate.equals(currentUserId)) {
                // Ensure candidate still exists
                if (userRepository.existsById(candidate)) {
                    peerId = candidate;
                    break;
                }
            }
        }

        if (peerId != null) {
            // MATCH FOUND! Create a Voice Room
            waitingTimestamps.remove(peerId);
            String roomId = "voice_" + UUID.randomUUID().toString().substring(0, 8);

            VoiceRoomSession session = new VoiceRoomSession(roomId, peerId, currentUserId);
            activeRooms.put(roomId, session);
            userToRoom.put(peerId, roomId);
            userToRoom.put(currentUserId, roomId);

            logger.info("Voice Match Room created: {} between user {} and user {}", roomId, peerId, currentUserId);

            // Notify Peer via WebSocket and SSE
            VoiceMatchStatusDto peerStatus = buildStatusForUser(session, peerId);
            VoiceMatchStatusDto currentStatus = buildStatusForUser(session, currentUserId);
            sendSseEvent(peerId, "MATCHED", peerStatus);
            sendSseEvent(currentUserId, "MATCHED", currentStatus);
            try {
                messagingTemplate.convertAndSend("/topic/voice-match/" + peerId, peerStatus);
            } catch (Exception e) {
                logger.warn("Could not notify peer {} via WebSocket: {}", peerId, e.getMessage());
            }

            return currentStatus;
        } else {
            // No peer available yet, add current user to waiting queue
            waitingQueue.add(currentUserId);
            waitingTimestamps.put(currentUserId, now);
            logger.info("User {} entered Voice Match queue (Queue size: {})", currentUserId, waitingQueue.size());
            return VoiceMatchStatusDto.waiting(waitingQueue.size());
        }
    }

    /**
     * Check current status (returns MATCHED if a peer connected, WAITING if in queue, or IDLE)
     */
    public VoiceMatchStatusDto getStatus(Long currentUserId) {
        String roomId = userToRoom.get(currentUserId);
        if (roomId != null) {
            VoiceRoomSession session = activeRooms.get(roomId);
            if (session != null) {
                if (isSessionExpired(session)) {
                    leaveRoom(currentUserId);
                    return VoiceMatchStatusDto.idle();
                }
                return buildStatusForUser(session, currentUserId);
            }
        }

        if (waitingQueue.contains(currentUserId)) {
            waitingTimestamps.put(currentUserId, System.currentTimeMillis());
            return VoiceMatchStatusDto.waiting(waitingQueue.size());
        }

        return VoiceMatchStatusDto.idle();
    }

    /**
     * Leave queue or exit active call
     */
    public void leaveRoom(Long currentUserId) {
        waitingQueue.remove(currentUserId);
        waitingTimestamps.remove(currentUserId);

        String roomId = userToRoom.remove(currentUserId);
        if (roomId != null) {
            VoiceRoomSession session = activeRooms.remove(roomId);
            if (session != null) {
                Long otherUser = session.getOtherUser(currentUserId);
                userToRoom.remove(otherUser);
                try {
                    VoiceMatchStatusDto ended = new VoiceMatchStatusDto(
                        "ENDED", roomId, null, "WEBRTC_P2P", null, null, false, false, false, null, session.createdAt, 60
                    );
                    messagingTemplate.convertAndSend("/topic/voice-match/" + otherUser, ended);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Send Match Request (Heart) during the call
     */
    public synchronized VoiceMatchStatusDto sendMatchRequest(Long currentUserId, String roomId) {
        VoiceRoomSession session = activeRooms.get(roomId);
        if (session == null) {
            throw new IllegalArgumentException("Voice match room not found or expired");
        }

        if (currentUserId.equals(session.user1Id)) {
            session.user1RequestedMatch = true;
        } else if (currentUserId.equals(session.user2Id)) {
            session.user2RequestedMatch = true;
        }

        Long peerId = session.getOtherUser(currentUserId);

        // Check if both users sent match request -> MUTUAL MATCH!
        if (session.user1RequestedMatch && session.user2RequestedMatch && !session.isMutualMatch) {
            session.isMutualMatch = true;

            try {
                // Record reciprocal swipes in database to create real Match & Conversation
                SwipeResponse swipeRes1 = matchingService.recordSwipe(session.user1Id, session.user2Id, true);
                SwipeResponse swipeRes2 = matchingService.recordSwipe(session.user2Id, session.user1Id, true);

                session.conversationId = swipeRes1.conversationId() != null
                    ? swipeRes1.conversationId()
                    : swipeRes2.conversationId();

                logger.info("MUTUAL VOICE MATCH! Room {}: users {} & {}, conversationId: {}",
                    roomId, session.user1Id, session.user2Id, session.conversationId);
            } catch (Exception e) {
                logger.error("Error creating mutual match in database: {}", e.getMessage(), e);
            }
        }

        // Notify peer of match request or mutual match
        VoiceMatchStatusDto peerStatus = buildStatusForUser(session, peerId);
        VoiceMatchStatusDto currentStatus = buildStatusForUser(session, currentUserId);
        sendSseEvent(peerId, session.isMutualMatch ? "MUTUAL_MATCH" : "PEER_MATCH_REQUEST", peerStatus);
        sendSseEvent(currentUserId, session.isMutualMatch ? "MUTUAL_MATCH" : "MATCH_REQUEST_SENT", currentStatus);
        try {
            messagingTemplate.convertAndSend("/topic/voice-match/" + peerId, peerStatus);
        } catch (Exception e) {
            logger.warn("Failed to broadcast match request to peer: {}", e.getMessage());
        }

        return currentStatus;
    }

    /**
     * Relay WebRTC audio signaling (Offer, Answer, ICE Candidate) between peers
     */
    public void relaySignal(String roomId, Long senderId, VoiceSignalDto signal) {
        VoiceRoomSession session = activeRooms.get(roomId);
        if (session == null) return;

        Long targetId = session.getOtherUser(senderId);
        VoiceSignalDto outgoing = new VoiceSignalDto(
            signal.type(),
            senderId,
            targetId,
            roomId,
            signal.payload()
        );

        // Send to targeted subscriber
        messagingTemplate.convertAndSend("/topic/voice-match/room/" + roomId + "/signals", outgoing);
        messagingTemplate.convertAndSend("/topic/voice-match/" + targetId + "/signal", outgoing);
    }

    private boolean isSessionExpired(VoiceRoomSession session) {
        // 60-second limit + 30-second buffer
        return (System.currentTimeMillis() - session.createdAt) > 95000;
    }

    private VoiceMatchStatusDto buildStatusForUser(VoiceRoomSession session, Long currentUserId) {
        Long peerId = session.getOtherUser(currentUserId);
        UserProfileDto peerProfile = null;
        try {
            peerProfile = profileService.getProfileByUserId(peerId);
        } catch (Exception e) {
            logger.warn("Could not load peer profile for {}: {}", peerId, e.getMessage());
        }

        boolean isLiveKit = livekitUrl != null && !livekitUrl.isBlank() &&
                            livekitApiKey != null && !livekitApiKey.isBlank() &&
                            livekitApiSecret != null && !livekitApiSecret.isBlank();

        String token = null;
        if (isLiveKit) {
            String userName = "User_" + currentUserId;
            if (peerProfile != null && peerProfile.displayName() != null) {
                userName = peerProfile.displayName();
            }
            token = generateLiveKitToken(session.roomId, currentUserId.toString(), userName);
        }

        boolean isInitiator = currentUserId.equals(session.user1Id);

        return new VoiceMatchStatusDto(
            "MATCHED",
            session.roomId,
            peerProfile,
            isLiveKit ? "LIVEKIT" : "WEBRTC_P2P",
            isLiveKit ? livekitUrl : null,
            token,
            isInitiator,
            session.peerRequestedMatch(currentUserId),
            session.isMutualMatch,
            session.conversationId,
            session.createdAt,
            60
        );
    }

    /**
     * Generate LiveKit compatible JWT Access Token
     */
    private String generateLiveKitToken(String roomId, String participantIdentity, String participantName) {
        try {
            byte[] secretBytes = livekitApiSecret.getBytes(StandardCharsets.UTF_8);
            SecretKey key = Keys.hmacShaKeyFor(secretBytes);

            Map<String, Object> videoGrants = new HashMap<>();
            videoGrants.put("room", roomId);
            videoGrants.put("roomJoin", true);
            videoGrants.put("canPublish", true);
            videoGrants.put("canSubscribe", true);

            long nowSec = System.currentTimeMillis() / 1000;

            return Jwts.builder()
                .header().add("alg", "HS256").and()
                .issuer(livekitApiKey)
                .subject(participantIdentity)
                .claim("name", participantName)
                .claim("video", videoGrants)
                .claim("nbf", nowSec - 5)
                .issuedAt(new Date(nowSec * 1000))
                .expiration(new Date((nowSec + 600) * 1000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        } catch (Exception e) {
            logger.error("LiveKit token generation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Subscribe to Server-Sent Events (SSE) for zero-polling real-time updates
     */
    public SseEmitter subscribeSse(Long userId) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes
        userEmitters.put(userId, emitter);

        emitter.onCompletion(() -> userEmitters.remove(userId));
        emitter.onTimeout(() -> {
            userEmitters.remove(userId);
            emitter.complete();
        });
        emitter.onError(e -> userEmitters.remove(userId));

        // Join queue on SSE connect
        VoiceMatchStatusDto initial = joinQueue(userId);
        sendSseEvent(userId, initial.status(), initial);

        return emitter;
    }

    private void sendSseEvent(Long userId, String eventName, Object data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                userEmitters.remove(userId);
            }
        }
    }
}
