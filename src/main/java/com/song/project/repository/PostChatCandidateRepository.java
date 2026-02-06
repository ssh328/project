package com.song.project.repository;

import com.song.project.entity.PostChatCandidate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostChatCandidateRepository extends JpaRepository<PostChatCandidate, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<PostChatCandidate> findByPost_IdAndUser_Id(Long postId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<PostChatCandidate> findByPost_IdOrderByCreatedAtDesc(Long postId);
}

