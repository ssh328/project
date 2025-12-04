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
    Optional<Likes> findByUserAndPost(User user, Post post);

    @EntityGraph(attributePaths = {"post"})
    List<Likes> findByUserId(Long id);

    @EntityGraph(attributePaths = {"post"})
    Page<Likes> findByUserId(Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"post"})
    List<Likes> findByPostId(Long postId);

    // postId로 모든 좋아요 삭제
    @Modifying
    @Query("DELETE FROM Likes l WHERE l.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
