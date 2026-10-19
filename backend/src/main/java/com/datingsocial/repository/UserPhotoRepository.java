package com.datingsocial.repository;

import com.datingsocial.model.UserPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {
    List<UserPhoto> findByProfileIdOrderBySortOrderAsc(Long profileId);
}
