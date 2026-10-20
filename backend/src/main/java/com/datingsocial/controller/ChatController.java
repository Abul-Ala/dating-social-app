package com.datingsocial.controller;

import com.datingsocial.dto.ConversationDto;
import com.datingsocial.dto.MessageDto;
import com.datingsocial.dto.SendMessageRequest;
import com.datingsocial.model.User;
import com.datingsocial.service.AuthService;
import com.datingsocial.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final AuthService authService;

    public ChatController(ChatService chatService, AuthService authService) {
        this.chatService = chatService;
        this.authService = authService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations() {
        User user = authService.getCurrentAuthenticatedUser();
        List<ConversationDto> conversations = chatService.getUserConversations(user.getId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        List<MessageDto> messages = chatService.getConversationMessages(id, user.getId());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
        @PathVariable Long id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        MessageDto message = chatService.sendMessage(id, user.getId(), request);
        return ResponseEntity.ok(message);
    }

    public record TypingStatusRequest(Boolean isTyping) {}

    @PostMapping("/conversations/{id}/typing")
    public ResponseEntity<Void> updateTypingStatus(
        @PathVariable Long id,
        @RequestParam(required = false) Boolean isTyping,
        @RequestBody(required = false) TypingStatusRequest body
    ) {
        boolean typing = (body != null && body.isTyping() != null)
            ? body.isTyping()
            : (isTyping != null ? isTyping : true);

        User user = authService.getCurrentAuthenticatedUser();
        chatService.sendTypingStatus(id, user.getId(), typing);
        return ResponseEntity.ok().build();
    }
}

