package com.datingsocial.controller;

import com.datingsocial.dto.InitiateVideoCallRequest;
import com.datingsocial.dto.VideoCallDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.VideoCallService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/video-call")
public class VideoCallController {

    private final VideoCallService videoCallService;
    private final AuthService authService;

    public VideoCallController(VideoCallService videoCallService, AuthService authService) {
        this.videoCallService = videoCallService;
        this.authService = authService;
    }

    /**
     * Start a Video Call with target user
     */
    @PostMapping("/initiate")
    public ResponseEntity<VideoCallDto> initiateCall(@Valid @RequestBody InitiateVideoCallRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        VideoCallDto callDto = videoCallService.initiateCall(user.getId(), request.targetUserId(), request.conversationId());
        return ResponseEntity.ok(callDto);
    }

    /**
     * Accept incoming video call
     */
    @PostMapping("/{callId}/accept")
    public ResponseEntity<VideoCallDto> acceptCall(@PathVariable String callId) {
        User user = authService.getCurrentAuthenticatedUser();
        VideoCallDto callDto = videoCallService.acceptCall(user.getId(), callId);
        return ResponseEntity.ok(callDto);
    }

    /**
     * Reject incoming video call
     */
    @PostMapping("/{callId}/reject")
    public ResponseEntity<Void> rejectCall(
        @PathVariable String callId,
        @RequestParam(required = false, defaultValue = "DECLINED") String reason
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        videoCallService.rejectCall(user.getId(), callId, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * End active or outgoing video call
     */
    @PostMapping("/{callId}/end")
    public ResponseEntity<Void> endCall(@PathVariable String callId) {
        User user = authService.getCurrentAuthenticatedUser();
        videoCallService.endCall(user.getId(), callId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if current user has an active ringing or connected call
     */
    @GetMapping("/active")
    public ResponseEntity<VideoCallDto> getActiveCall() {
        User user = authService.getCurrentAuthenticatedUser();
        VideoCallDto callDto = videoCallService.getActiveCall(user.getId());
        return ResponseEntity.ok(callDto);
    }

    /**
     * Real-time Server-Sent Events stream for video call alerts
     */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVideoEvents() {
        User user = authService.getCurrentAuthenticatedUser();
        return videoCallService.subscribeSse(user.getId());
    }
}
