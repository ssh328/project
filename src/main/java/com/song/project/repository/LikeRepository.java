package com.song.project.repository;

import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Likes, Long> {
    // 사용자와 게시물 조회
    Optional<Likes> findByUserAndPost(User user, Post post);

    // 사용자 ID로 좋아요 목록 조회
    @EntityGraph(attributePaths = {"post"})
    List<Likes> findByUserId(Long id);

    // 사용자 ID로 좋아요 목록 페이지 조회
    @EntityGraph(attributePaths = {"post"})
    Page<Likes> findByUserId(Long id, Pageable pageable);

    // 게시물 ID로 좋아요 목록 조회
    @EntityGraph(attributePaths = {"post"})
    List<Likes> findByPostId(Long postId);

    // 게시물 ID로 모든 좋아요 삭제
    @Modifying
    @Query("DELETE FROM Likes l WHERE l.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
