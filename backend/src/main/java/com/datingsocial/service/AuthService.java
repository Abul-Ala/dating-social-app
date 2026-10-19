package com.datingsocial.service;

import com.datingsocial.dto.*;
import com.datingsocial.model.*;
import com.datingsocial.repository.InterestRepository;
import com.datingsocial.repository.UserProfileRepository;
import com.datingsocial.repository.UserRepository;
import com.datingsocial.security.JwtUtils;
import com.datingsocial.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final InterestRepository interestRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ProfileService profileService;
    private final UserActivityTracker userActivityTracker;

    public AuthService(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        InterestRepository interestRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtUtils jwtUtils,
        ProfileService profileService,
        UserActivityTracker userActivityTracker
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.interestRepository = interestRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.profileService = profileService;
        this.userActivityTracker = userActivityTracker;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        userActivityTracker.recordActivity(user.getId());
        String token = jwtUtils.generateToken(user.getId(), user.getEmail());
        UserProfileDto profileDto = user.getProfile() != null
            ? profileService.toDto(user.getProfile(), null, 100)
            : null;

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole(), profileDto);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        // Age verification check (18+)
        int age = Period.between(request.birthDate(), LocalDate.now()).getYears();
        if (age < 18) {
            throw new IllegalArgumentException("You must be at least 18 years old to join.");
        }

        User user = new User(
            request.email(),
            passwordEncoder.encode(request.password()),
            null
        );
        user = userRepository.save(user);

        UserProfile profile = new UserProfile(
            user,
            request.displayName(),
            request.birthDate(),
            request.gender(),
            request.interestedIn() != null ? request.interestedIn() : GenderPreference.EVERYONE
        );
        profile.setCity(request.city() != null ? request.city() : "Mumbai, India");
        profile.setLatitude(request.latitude() != null ? request.latitude() : 19.0760);
        profile.setLongitude(request.longitude() != null ? request.longitude() : 72.8777);

        // Add initial photo if provided
        if (request.initialPhotoUrl() != null && !request.initialPhotoUrl().isBlank()) {
            UserPhoto photo = new UserPhoto(request.initialPhotoUrl(), true, 0);
            profile.addPhoto(photo);
        }

        // Add interests if provided
        if (request.interestIds() != null && !request.interestIds().isEmpty()) {
            Set<Interest> interests = new HashSet<>(interestRepository.findAllById(request.interestIds()));
            profile.setInterests(interests);
        }

        profile = userProfileRepository.save(profile);
        user.setProfile(profile);

        userActivityTracker.recordActivity(user.getId());
        String token = jwtUtils.generateToken(user.getId(), user.getEmail());
        UserProfileDto profileDto = profileService.toDto(profile, null, 100);

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getRole(), profileDto);
    }

    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("No authenticated user found in security context");
        }
        return userRepository.findById(principal.getId())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}
