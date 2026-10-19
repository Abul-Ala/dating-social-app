package com.datingsocial.controller;

import com.datingsocial.dto.InterestDto;
import com.datingsocial.dto.UpdateProfileRequest;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @GetMapping("/profiles/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(profileService.getProfileByUserId(user.getId()));
    }

    @PutMapping("/profiles/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(profileService.updateProfile(user.getId(), request));
    }

    @GetMapping("/profiles/{userId}")
    public ResponseEntity<UserProfileDto> getProfileById(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getProfileByUserId(userId));
    }

    @PostMapping("/profiles/me/photos")
    public ResponseEntity<UserProfileDto> addPhoto(
        @RequestParam String photoUrl,
        @RequestParam(defaultValue = "false") boolean isPrimary
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(profileService.addPhoto(user.getId(), photoUrl, isPrimary));
    }

    @PostMapping(value = "/profiles/me/photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDto> uploadPhoto(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "isPrimary", defaultValue = "true") boolean isPrimary
    ) throws IOException {
        User user = authService.getCurrentAuthenticatedUser();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded photo is empty");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "photo_" + user.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path uploadsDir = Paths.get("uploads");
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        Path targetPath = uploadsDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String photoUrl = "/uploads/" + filename;
        return ResponseEntity.ok(profileService.addPhoto(user.getId(), photoUrl, isPrimary));
    }

    @DeleteMapping("/profiles/me/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long photoId) {
        User user = authService.getCurrentAuthenticatedUser();
        profileService.deletePhoto(user.getId(), photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/interests")
    public ResponseEntity<List<InterestDto>> getInterests() {
        return ResponseEntity.ok(profileService.getAllInterests());
    }
}
