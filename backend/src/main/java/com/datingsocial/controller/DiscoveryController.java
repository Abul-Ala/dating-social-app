package com.datingsocial.controller;

import com.datingsocial.dto.SwipeRequest;
import com.datingsocial.dto.SwipeResponse;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.Gender;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.DiscoveryService;
import com.datingsocial.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final MatchingService matchingService;
    private final AuthService authService;

    public DiscoveryController(
        DiscoveryService discoveryService,
        MatchingService matchingService,
        AuthService authService
    ) {
        this.discoveryService = discoveryService;
        this.matchingService = matchingService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<UserProfileDto>> getDiscoveryFeed(
        @RequestParam(required = false) Integer minAge,
        @RequestParam(required = false) Integer maxAge,
        @RequestParam(required = false) Gender gender,
        @RequestParam(required = false) Double maxDistanceKm
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        List<UserProfileDto> candidates = discoveryService.getDiscoveryFeed(
            user.getId(), minAge, maxAge, gender, maxDistanceKm
        );
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/swipe")
    public ResponseEntity<SwipeResponse> swipe(@Valid @RequestBody SwipeRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        SwipeResponse response = matchingService.recordSwipe(
            user.getId(),
            request.targetUserId(),
            request.isLike()
        );
        return ResponseEntity.ok(response);
    }
}
