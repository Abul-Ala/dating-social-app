package com.datingsocial.repository;

import com.datingsocial.model.Story;
import com.datingsocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);

    List<Story> findByUserAndExpiresAtAfterOrderByCreatedAtDesc(User user, LocalDateTime now);

    List<Story> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now AND (s.user.id NOT IN :excludedUserIds) ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesExcludingUsers(
        @Param("now") LocalDateTime now,
        @Param("excludedUserIds") List<Long> excludedUserIds
    );
}
