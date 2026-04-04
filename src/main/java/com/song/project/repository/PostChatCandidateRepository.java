package com.song.project.repository;

import com.song.project.entity.PostChatCandidate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostChatCandidateRepository extends JpaRepository<PostChatCandidate, Long> {
    // 게시물에 해당 사용자가 채팅 후보로 이미 등록되어 있는지 여부
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    // 해당 게시글 + 사용자 조합의 후보 레코드 한 건 조회
    Optional<PostChatCandidate> findByPost_IdAndUser_Id(Long postId, Long userId);

    // 해당 게시글의 채팅 후보 목록을 최신순으로 조회 (직거래 판매완료 시 구매자 선택용)
    @EntityGraph(attributePaths = {"user"})
    List<PostChatCandidate> findByPost_IdOrderByCreatedAtDesc(Long postId);

    @Modifying
    @Query("DELETE FROM PostChatCandidate p WHERE p.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}

