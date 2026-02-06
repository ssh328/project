package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 특정 게시글에 대해 "채팅을 시작한 사용자"를 후보로 기록하는 테이블
 * - TalkJS를 사용하므로 실제 메시지를 DB에 저장하지 않지만,
 *   최소한의 "관심 표현(채팅 진입)"을 서버에 남겨 판매완료 시 구매자 후보로 활용한다.
 */
@Entity
@Getter
@Setter
@ToString
@Table(
    name = "post_chat_candidate",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_candidate_post_user", columnNames = {"postId", "userId"})
    },
    indexes = {
        @Index(name = "idx_post_candidate_post", columnList = "postId"),
        @Index(name = "idx_post_candidate_user", columnList = "userId")
    }
)
public class PostChatCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

