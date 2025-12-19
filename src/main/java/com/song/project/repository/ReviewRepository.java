package com.song.project.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.song.project.entity.Review;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 대상자 ID로 리뷰 목록 조회
    @EntityGraph(attributePaths = {"reviewer","targetUser"})
    Page<Review> findByTargetUser_Id(Long targetUserId, Pageable pageable);
}