package com.datingsocial.repository;

import com.datingsocial.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :userId UNION SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :userId")
    List<Long> findAllBlockedUserIdsRelatedTo(@Param("userId") Long userId);

    List<Block> findByBlockerId(Long blockerId);
}
