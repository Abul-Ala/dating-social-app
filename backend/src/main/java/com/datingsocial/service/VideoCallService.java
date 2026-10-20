package com.datingsocial.service;

import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.dto.VideoCallDto;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoCallService {

    private static final Logger logger = LoggerFactory.getLogger(VideoCallService.class);

    private final UserRepository userRepository;
    private final ProfileService profileService;
    private final UserActivityTracker userActivityTracker;
    private final SimpMessagingTemplate messagingTemplate;
    private final AppEventService appEventService;

    @Value("${app.livekit.url:wss://vibly-dukzgaut.livekit.cloud}")
    private String livekitUrl;

    @Value("${app.livekit.api-key:APIDEJNCYET5MCu}")
    private String livekitApiKey;

    @Value("${app.livekit.api-secret:1znSRh2ffVpP71Wfs5Zm1f9OgKEQy9FMuTzBoLdGG4cB}")
    private String livekitApiSecret;

    // Active calls: callId -> Session
    private final ConcurrentHashMap<String, VideoCallSession> activeCalls = new ConcurrentHashMap<>();
    // User ID -> callId
    private final ConcurrentHashMap<Long, String> userToCall = new ConcurrentHashMap<>();
    // User ID -> SseEmitter for real-time video notifications
    private final ConcurrentHashMap<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public static class VideoCallSession {
        public final String callId;
        public final String roomId;
        public final Long callerId;
        public final Long targetId;
        public final Long conversationId;
        public volatile String status; // "RINGING", "ACCEPTED", "REJECTED", "ENDED", "BUSY", "MISSED"
        public final long createdAt;
        public volatile long acceptedAt;

        public VideoCallSession(String callId, String roomId, Long callerId, Long targetId, Long conversationId) {
            this.callId = callId;
            this.roomId = roomId;
            this.callerId = callerId;
            this.targetId = targetId;
            this.conversationId = conversationId;
            this.status = "RINGING";
            this.createdAt = System.currentTimeMillis();
        }

        public Long getOtherUser(Long currentUserId) {
            return currentUserId.equals(callerId) ? targetId : callerId;
        }

        public boolean isParticipant(Long currentUserId) {
            return currentUserId.equals(callerId) || currentUserId.equals(targetId);
        }
    }

    public VideoCallService(
        UserRepository userRepository,
        ProfileService profileService,
        UserActivityTracker userActivityTracker,
        SimpMessagingTemplate messagingTemplate,
        AppEventService appEventService
    ) {
        this.userRepository = userRepository;
        this.profileService = profileService;
        this.userActivityTracker = userActivityTracker;
        this.messagingTemplate = messagingTemplate;
        this.appEventService = appEventService;
    }

    /**
     * Check if a user is online (via active SSE connection or recent activity within 3 minutes)
     */
    public boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        if (userEmitters.containsKey(userId)) return true;
        return userActivityTracker.isOnline(userId);
    }

    /**
     * Initiate a 1-on-1 Video Call
     */
    public synchronized VideoCallDto initiateCall(Long callerId, Long targetUserId, Long conversationId) {
        if (callerId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot call yourself");
        }

        if (!userRepository.existsById(targetUserId)) {
            throw new IllegalArgumentException("Target user does not exist");
        }

        // Clean stale sessions (> 45s ringing without answer)
        cleanStaleCalls();

        // Check if target is already in an ongoing call
        String existingCallId = userToCall.get(targetUserId);
        if (existingCallId != null) {
            VideoCallSession existingSession = activeCalls.get(existingCallId);
            if (existingSession != null && ("RINGING".equals(existingSession.status) || "ACCEPTED".equals(existingSession.status))) {
                // User is busy
                return buildCallDto(
                    new VideoCallSession("busy", "busy", callerId, targetUserId, conversationId),
                    callerId,
                    "BUSY"
                );
            }
        }

        // If caller was already in a call, clear it
        String callerExistingCall = userToCall.remove(callerId);
        if (callerExistingCall != null) {
            activeCalls.remove(callerExistingCall);
        }

        String callId = "call_" + UUID.randomUUID().toString().substring(0, 8);
        String roomId = "vid_" + (conversationId != null ? conversationId : "call") + "_" + UUID.randomUUID().toString().substring(0, 6);

        VideoCallSession session = new VideoCallSession(callId, roomId, callerId, targetUserId, conversationId);
        activeCalls.put(callId, session);
        userToCall.put(callerId, callId);
        userToCall.put(targetUserId, callId);

        boolean online = isUserOnline(targetUserId);

        logger.info("Initiated Video Call {} between caller {} and target {} (Online: {})",
            callId, callerId, targetUserId, online);

        // Build status for both caller and target
        VideoCallDto callerDto = buildCallDto(session, callerId, "RINGING");
        VideoCallDto targetDto = buildCallDto(session, targetUserId, "RINGING");

        // Real-time dispatch to target recipient (Ringing!)
        sendSseEvent(targetUserId, "INCOMING_CALL", targetDto);
        try {
            messagingTemplate.convertAndSend("/topic/video-call/" + targetUserId, targetDto);
        } catch (Exception e) {
            logger.warn("WebSocket dispatch to target {} failed: {}", targetUserId, e.getMessage());
        }

        return callerDto;
    }

    /**
     * Accept incoming video call
     */
    public synchronized VideoCallDto acceptCall(Long userId, String callId) {
        VideoCallSession session = activeCalls.get(callId);
        if (session == null) {
            throw new IllegalArgumentException("Call session not found or already ended");
        }

        if (!userId.equals(session.targetId)) {
            throw new SecurityException("Only the target recipient can accept this call");
        }

        session.status = "ACCEPTED";
        session.acceptedAt = System.currentTimeMillis();

        logger.info("Video Call {} ACCEPTED by target {}", callId, userId);

        VideoCallDto callerDto = buildCallDto(session, session.callerId, "ACCEPTED");
        VideoCallDto targetDto = buildCallDto(session, session.targetId, "ACCEPTED");

        // Notify caller that call was accepted
        sendSseEvent(session.callerId, "CALL_ACCEPTED", callerDto);
        sendSseEvent(session.targetId, "CALL_ACCEPTED", targetDto);
        try {
            messagingTemplate.convertAndSend("/topic/video-call/" + session.callerId, callerDto);
            messagingTemplate.convertAndSend("/topic/video-call/" + session.targetId, targetDto);
        } catch (Exception e) {
            logger.warn("WebSocket broadcast failed for acceptCall: {}", e.getMessage());
        }

        return targetDto;
    }

    /**
     * Reject incoming video call
     */
    public synchronized void rejectCall(Long userId, String callId, String reason) {
        VideoCallSession session = activeCalls.get(callId);
        if (session == null) return;

        session.status = "REJECTED";
        Long otherUser = session.getOtherUser(userId);

        logger.info("Video Call {} REJECTED by user {} (Reason: {})", callId, userId, reason);

        VideoCallDto dto = buildCallDto(session, otherUser, "REJECTED");
        sendSseEvent(otherUser, "CALL_REJECTED", dto);
        try {
            messagingTemplate.convertAndSend("/topic/video-call/" + otherUser, dto);
        } catch (Exception e) {
            // ignore
        }

        cleanupSession(session);
    }

    /**
     * End active or ringing video call
     */
    public synchronized void endCall(Long userId, String callId) {
        VideoCallSession session = activeCalls.get(callId);
        if (session == null) {
            // Also check if user has a mapped call
            String userMappedCall = userToCall.remove(userId);
            if (userMappedCall != null) {
                session = activeCalls.get(userMappedCall);
            }
        }

        if (session == null) return;

        session.status = "ENDED";
        Long otherUser = session.getOtherUser(userId);

        logger.info("Video Call {} ENDED by user {}", callId, userId);

        VideoCallDto dto = buildCallDto(session, otherUser, "ENDED");
        sendSseEvent(otherUser, "CALL_ENDED", dto);
        sendSseEvent(userId, "CALL_ENDED", buildCallDto(session, userId, "ENDED"));

        try {
            messagingTemplate.convertAndSend("/topic/video-call/" + otherUser, dto);
            messagingTemplate.convertAndSend("/topic/video-call/" + userId, dto);
        } catch (Exception e) {
            // ignore
        }

        cleanupSession(session);
    }

    /**
     * Get active ringing or ongoing call for user (if any)
     */
    public VideoCallDto getActiveCall(Long userId) {
        cleanStaleCalls();

        String callId = userToCall.get(userId);
        if (callId != null) {
            VideoCallSession session = activeCalls.get(callId);
            if (session != null) {
                return buildCallDto(session, userId, session.status);
            } else {
                userToCall.remove(userId);
            }
        }
        return null;
    }

    /**
     * Real-time Server-Sent Events subscription for video calls
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

        // Check if there is an active call awaiting user
        VideoCallDto active = getActiveCall(userId);
        if (active != null) {
            sendSseEvent(userId, "ACTIVE_CALL", active);
        }

        return emitter;
    }

    private void cleanupSession(VideoCallSession session) {
        if (session != null) {
            activeCalls.remove(session.callId);
            userToCall.remove(session.callerId);
            userToCall.remove(session.targetId);
        }
    }

    private void cleanStaleCalls() {
        long now = System.currentTimeMillis();
        activeCalls.entrySet().removeIf(entry -> {
            VideoCallSession session = entry.getValue();
            if ("RINGING".equals(session.status) && (now - session.createdAt) > 45000) {
                userToCall.remove(session.callerId);
                userToCall.remove(session.targetId);
                sendSseEvent(session.callerId, "CALL_MISSED", buildCallDto(session, session.callerId, "MISSED"));
                sendSseEvent(session.targetId, "CALL_MISSED", buildCallDto(session, session.targetId, "MISSED"));
                return true;
            }
            if ("ENDED".equals(session.status) || "REJECTED".equals(session.status)) {
                userToCall.remove(session.callerId);
                userToCall.remove(session.targetId);
                return true;
            }
            return false;
        });
    }

    private VideoCallDto buildCallDto(VideoCallSession session, Long forUserId, String status) {
        UserProfileDto callerProfile = null;
        UserProfileDto targetProfile = null;
        try {
            callerProfile = profileService.getProfileByUserId(session.callerId);
        } catch (Exception e) {
            logger.warn("Could not load caller profile: {}", e.getMessage());
        }
        try {
            targetProfile = profileService.getProfileByUserId(session.targetId);
        } catch (Exception e) {
            logger.warn("Could not load target profile: {}", e.getMessage());
        }

        String token = null;
        if (livekitApiKey != null && !livekitApiKey.isBlank() &&
            livekitApiSecret != null && !livekitApiSecret.isBlank()) {
            String userName = "User_" + forUserId;
            if (forUserId.equals(session.callerId) && callerProfile != null && callerProfile.displayName() != null) {
                userName = callerProfile.displayName();
            } else if (forUserId.equals(session.targetId) && targetProfile != null && targetProfile.displayName() != null) {
                userName = targetProfile.displayName();
            }
            token = generateLiveKitToken(session.roomId, forUserId.toString(), userName);
        }

        boolean targetOnline = isUserOnline(session.targetId);

        return new VideoCallDto(
            session.callId,
            session.roomId,
            session.callerId,
            callerProfile,
            session.targetId,
            targetProfile,
            session.conversationId,
            status,
            livekitUrl,
            token,
            targetOnline,
            session.createdAt
        );
    }

    /**
     * Generate LiveKit compatible JWT Access Token with camera & microphone video grants
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
                .expiration(new Date((nowSec + 1800) * 1000)) // 30 minutes video call validity
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        } catch (Exception e) {
            logger.error("LiveKit video call token generation failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private void sendSseEvent(Long userId, String eventName, Object data) {
        // Dispatch to unified app event stream
        try {
            appEventService.sendEvent(userId, eventName, data);
        } catch (Exception ignored) {}

        // Also dispatch to local emitters for backward compatibility
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
