package com.datingsocial.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_matches",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
    }
)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(nullable = false, updatable = false)
    private LocalDateTime matchedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToOne(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Conversation conversation;

    public Match() {}

    public Match(User userA, User userB) {
        // Normalize ordering by id so (A, B) and (B, A) represent the exact same record
        if (userA.getId() != null && userB.getId() != null && userA.getId() > userB.getId()) {
            this.user1 = userB;
            this.user2 = userA;
        } else {
            this.user1 = userA;
            this.user2 = userB;
        }
        this.matchedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getOtherUser(Long currentUserId) {
        if (user1 != null && user1.getId().equals(currentUserId)) {
            return user2;
        }
        return user1;
    }
}
