package com.datingsocial.repository;

import com.datingsocial.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByMatchId(Long matchId);

    @Query("SELECT c FROM Conversation c WHERE (c.match.user1.id = :userId OR c.match.user2.id = :userId) " +
           "AND c.match.isActive = true ORDER BY c.updatedAt DESC")
    List<Conversation> findAllByUserId(@Param("userId") Long userId);
}
