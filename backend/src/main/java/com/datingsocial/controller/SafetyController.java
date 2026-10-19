package com.datingsocial.controller;

import com.datingsocial.dto.BlockRequest;
import com.datingsocial.dto.ReportRequest;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.SafetyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/safety")
public class SafetyController {

    private final SafetyService safetyService;
    private final AuthService authService;

    public SafetyController(SafetyService safetyService, AuthService authService) {
        this.safetyService = safetyService;
        this.authService = authService;
    }

    @PostMapping("/block")
    public ResponseEntity<Map<String, String>> blockUser(@Valid @RequestBody BlockRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        safetyService.blockUser(user.getId(), request.targetUserId(), request.reason());
        return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
    }

    @PostMapping("/unmatch/{targetUserId}")
    public ResponseEntity<Map<String, String>> unmatch(@PathVariable Long targetUserId) {
        User user = authService.getCurrentAuthenticatedUser();
        safetyService.unmatchUser(user.getId(), targetUserId);
        return ResponseEntity.ok(Map.of("message", "Unmatched successfully"));
    }

    @PostMapping("/report")
    public ResponseEntity<Map<String, String>> reportUser(@Valid @RequestBody ReportRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        safetyService.reportUser(user.getId(), request.targetUserId(), request.reason(), request.details());
        return ResponseEntity.ok(Map.of("message", "Report submitted. Target user has been blocked."));
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<UserProfileDto>> getBlockedUsers() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(safetyService.getBlockedUsers(user.getId()));
    }
}
