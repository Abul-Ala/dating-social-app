package com.datingsocial.controller;

import com.datingsocial.dto.StoryDto;
import com.datingsocial.dto.UserStoryGroupDto;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.StoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {

    private final StoryService storyService;
    private final AuthService authService;

    public StoryController(StoryService storyService, AuthService authService) {
        this.storyService = storyService;
        this.authService = authService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoryDto> createStory(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "caption", required = false) String caption
    ) throws IOException {
        User user = authService.getCurrentAuthenticatedUser();
        StoryDto created = storyService.createStory(user.getId(), file, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/tray")
    public ResponseEntity<List<UserStoryGroupDto>> getStoriesTray() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(storyService.getStoriesTray(user.getId()));
    }

    @GetMapping("/me")
    public ResponseEntity<List<StoryDto>> getMyStories() {
        User user = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(storyService.getMyStories(user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        storyService.deleteStory(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordStoryView(@PathVariable Long id) {
        storyService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
