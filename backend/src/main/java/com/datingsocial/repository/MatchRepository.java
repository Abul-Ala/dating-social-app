package com.datingsocial.repository;

import com.datingsocial.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("SELECT m FROM Match m WHERE ((m.user1.id = :userA AND m.user2.id = :userB) OR (m.user1.id = :userB AND m.user2.id = :userA)) AND m.isActive = true")
    Optional<Match> findActiveMatchBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userId OR m.user2.id = :userId) AND m.isActive = true ORDER BY m.matchedAt DESC")
    List<Match> findAllActiveMatchesForUser(@Param("userId") Long userId);
}
