package com.datingsocial.controller;

import com.datingsocial.model.User;
import com.datingsocial.service.AppEventService;
import com.datingsocial.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller providing a unified Server-Sent Events stream (/api/v1/events)
 * for real-time notifications across chat, likes, matches, stories, and calls.
 */
@RestController
@RequestMapping("/api/v1/events")
public class AppEventController {

    private final AppEventService appEventService;
    private final AuthService authService;

    public AppEventController(AppEventService appEventService, AuthService authService) {
        this.appEventService = appEventService;
        this.authService = authService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAppEvents() {
        User user = authService.getCurrentAuthenticatedUser();
        return appEventService.subscribe(user.getId());
    }
}
