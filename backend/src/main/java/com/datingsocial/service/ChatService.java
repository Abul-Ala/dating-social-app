package com.datingsocial.service;

import com.datingsocial.dto.ConversationDto;
import com.datingsocial.dto.MessageDto;
import com.datingsocial.dto.SendMessageRequest;
import com.datingsocial.dto.UserProfileDto;
import com.datingsocial.model.*;
import com.datingsocial.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BlockRepository blockRepository;
    private final ProfileService profileService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
        ConversationRepository conversationRepository,
        MessageRepository messageRepository,
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        BlockRepository blockRepository,
        ProfileService profileService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.blockRepository = blockRepository;
        this.profileService = profileService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
        List<ConversationDto> result = new ArrayList<>();

        for (Conversation c : conversations) {
            Match match = c.getMatch();
            User otherUser = match.getOtherUser(userId);

            // Skip if blocked
            if (blockRepository.existsByBlockerIdAndBlockedId(userId, otherUser.getId()) ||
                blockRepository.existsByBlockerIdAndBlockedId(otherUser.getId(), userId)) {
                continue;
            }

            UserProfile otherProfile = userProfileRepository.findByUserId(otherUser.getId()).orElse(null);
            UserProfileDto otherDto = otherProfile != null ? profileService.toDto(otherProfile, null, 90) : null;

            List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
            String lastMsg = messages.isEmpty() ? "Matched! Say hello \uD83D\uDC4B" : messages.get(messages.size() - 1).getContent();
            LocalDateTime lastTime = messages.isEmpty() ? c.getCreatedAt() : messages.get(messages.size() - 1).getCreatedAt();
            long unread = messageRepository.countUnreadMessages(c.getId(), userId);

            result.add(new ConversationDto(
                c.getId(),
                match.getId(),
                otherDto,
                lastMsg,
                lastTime,
                unread,
                c.getUpdatedAt()
            ));
        }

        return result;
    }

    @Transactional
    public List<MessageDto> getConversationMessages(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        validateUserInConversation(conversation, userId);

        // Mark incoming messages as read
        messageRepository.markAllAsRead(conversationId, userId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream().map(this::toDto).toList();
    }

    @Transactional
    public MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        validateUserInConversation(conversation, senderId);

        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = conversation.getMatch().getOtherUser(senderId);

        // Verify not blocked and match is active
        if (blockRepository.existsByBlockerIdAndBlockedId(senderId, receiver.getId()) ||
            blockRepository.existsByBlockerIdAndBlockedId(receiver.getId(), senderId) ||
            !conversation.getMatch().isActive()) {
            throw new IllegalStateException("Cannot send message: This user is blocked or match is inactive.");
        }

        Message message = new Message(conversation, sender, request.content(), request.type());
        message = messageRepository.save(message);

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        MessageDto messageDto = toDto(message);

        // Broadcast to WebSocket subscribers for this conversation
        try {
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, messageDto);
            messagingTemplate.convertAndSend("/topic/user/" + receiver.getId() + "/messages", messageDto);
        } catch (Exception e) {
            // Background push notification fallback
        }

        return messageDto;
    }

    private void validateUserInConversation(Conversation conversation, Long userId) {
        Match match = conversation.getMatch();
        if (match == null || !match.isActive()) {
            throw new IllegalStateException("This match is no longer active.");
        }
        boolean isParticipant = match.getUser1().getId().equals(userId) || match.getUser2().getId().equals(userId);
        if (!isParticipant) {
            throw new SecurityException("User is not a participant in this conversation.");
        }
    }

    public MessageDto toDto(Message message) {
        String senderName = "Unknown";
        if (message.getSender().getProfile() != null) {
            senderName = message.getSender().getProfile().getDisplayName();
        }
        return new MessageDto(
            message.getId(),
            message.getConversation().getId(),
            message.getSender().getId(),
            senderName,
            message.getContent(),
            message.getType(),
            message.isRead(),
            message.getCreatedAt()
        );
    }
}
