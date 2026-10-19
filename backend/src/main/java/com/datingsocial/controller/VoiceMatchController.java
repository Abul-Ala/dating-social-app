package com.datingsocial.controller;

import com.datingsocial.dto.VoiceMatchStatusDto;
import com.datingsocial.dto.VoiceSignalDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.VoiceMatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/voice-match")
public class VoiceMatchController {

    private final VoiceMatchService voiceMatchService;
    private final AuthService authService;

    public VoiceMatchController(VoiceMatchService voiceMatchService, AuthService authService) {
        this.voiceMatchService = voiceMatchService;
        this.authService = authService;
    }

    /**
     * Real-time Server-Sent Events (SSE) stream: replaces polling with a single persistent push stream!
     */
    @GetMapping(value = "/queue/events", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamQueueEvents() {
        User user = authService.getCurrentAuthenticatedUser();
        return voiceMatchService.subscribeSse(user.getId());
    }

    /**
     * Enter voice match pool. If a peer is already waiting, pairs immediately!
     */
    @PostMapping("/queue/join")
    public ResponseEntity<VoiceMatchStatusDto> joinQueue() {
        User user = authService.getCurrentAuthenticatedUser();
        VoiceMatchStatusDto status = voiceMatchService.joinQueue(user.getId());
        return ResponseEntity.ok(status);
    }

    /**
     * Poll queue status / call status
     */
    @GetMapping("/queue/status")
    public ResponseEntity<VoiceMatchStatusDto> getStatus() {
        User user = authService.getCurrentAuthenticatedUser();
        VoiceMatchStatusDto status = voiceMatchService.getStatus(user.getId());
        return ResponseEntity.ok(status);
    }

    /**
     * Cancel search or leave active call
     */
    @PostMapping("/queue/leave")
    public ResponseEntity<Void> leaveQueue() {
        User user = authService.getCurrentAuthenticatedUser();
        voiceMatchService.leaveRoom(user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Send Match Request (Heart) in active room
     */
    @PostMapping("/room/{roomId}/match-request")
    public ResponseEntity<VoiceMatchStatusDto> sendMatchRequest(@PathVariable String roomId) {
        User user = authService.getCurrentAuthenticatedUser();
        VoiceMatchStatusDto result = voiceMatchService.sendMatchRequest(user.getId(), roomId);
        return ResponseEntity.ok(result);
    }

    /**
     * Relay WebRTC audio signal (Offer, Answer, ICE Candidate)
     */
    @PostMapping("/room/{roomId}/signal")
    public ResponseEntity<Void> sendSignal(
        @PathVariable String roomId,
        @RequestBody VoiceSignalDto signal
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        voiceMatchService.relaySignal(roomId, user.getId(), signal);
        return ResponseEntity.ok().build();
    }

    /**
     * WebSocket-based instant WebRTC signaling relay
     */
    @MessageMapping("/voice-match/{roomId}/signal")
    public void handleWsSignal(
        @DestinationVariable String roomId,
        @Payload VoiceSignalDto signal,
        Principal principal
    ) {
        if (principal != null) {
            try {
                Long userId = Long.parseLong(principal.getName());
                voiceMatchService.relaySignal(roomId, userId, signal);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
