package com.datingsocial.repository;

import com.datingsocial.model.Gender;
import com.datingsocial.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    @Query("SELECT p FROM UserProfile p WHERE p.user.id != :currentUserId AND p.user.status = 'ACTIVE' " +
           "AND p.user.id NOT IN :excludedUserIds " +
           "AND (:gender IS NULL OR p.gender = :gender)")
    List<UserProfile> findDiscoveryCandidates(
        @Param("currentUserId") Long currentUserId,
        @Param("excludedUserIds") List<Long> excludedUserIds,
        @Param("gender") Gender gender
    );
}
