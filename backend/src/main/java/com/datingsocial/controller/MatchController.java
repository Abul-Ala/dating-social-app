package com.datingsocial.controller;

import com.datingsocial.dto.MatchDto;
import com.datingsocial.dto.LikeNotificationDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchingService matchingService;
    private final AuthService authService;

    public MatchController(MatchingService matchingService, AuthService authService) {
        this.matchingService = matchingService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<MatchDto>> getMatches() {
        User user = authService.getCurrentAuthenticatedUser();
        List<MatchDto> matches = matchingService.getUserMatches(user.getId());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/likes-received")
    public ResponseEntity<List<LikeNotificationDto>> getLikesReceived() {
        User user = authService.getCurrentAuthenticatedUser();
        List<LikeNotificationDto> likes = matchingService.getLikesReceived(user.getId());
        return ResponseEntity.ok(likes);
    }
}
