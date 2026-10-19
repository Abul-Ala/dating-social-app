package com.datingsocial.repository;

import com.datingsocial.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByLikerIdAndLikedId(Long likerId, Long likedId);

    boolean existsByLikerIdAndLikedIdAndIsLikeTrue(Long likerId, Long likedId);

    @Query("SELECT l.liked.id FROM Like l WHERE l.liker.id = :likerId")
    List<Long> findSwipedUserIdsByLikerId(@Param("likerId") Long likerId);

    @Query("SELECT l FROM Like l WHERE l.liked.id = :userId AND l.isLike = true " +
           "AND NOT EXISTS (SELECT 1 FROM Like l2 WHERE l2.liker.id = :userId AND l2.liked.id = l.liker.id) " +
           "ORDER BY l.createdAt DESC")
    List<Like> findLikesReceivedByUserId(@Param("userId") Long userId);
}
