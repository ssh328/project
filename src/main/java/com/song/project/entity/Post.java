package com.song.project.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(
    indexes = {
        @Index(columnList = "title", name = "idx_post_title"),
        @Index(columnList = "userId", name = "idx_post_userId"),
        @Index(columnList = "category", name = "idx_post_category"),
        @Index(columnList = "status", name = "idx_post_status"),
        @Index(columnList = "created", name = "idx_post_created"),
        @Index(columnList = "likeCnt", name = "idx_post_likeCnt")
    }
)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;    // 게시물 제목

    @Column(columnDefinition = "TEXT")
    private String body;    // 게시물 내용

    @Column(nullable = false)
    private Integer price;    // 게시물 가격

    @Column(nullable = false)
    private String category;    // 게시물 카테고리

    @CreationTimestamp
    private LocalDateTime created;    // 게시물 작성일시

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer likeCnt = 0;    // 게시물 좋아요 수

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount = 0L;    // 게시물 조회수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ON_SALE'")
    private PostStatus status = PostStatus.ON_SALE;    // 게시물 상태

    // 하나의 Post는 여러 이미지 소유
    @ToString.Exclude
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<PostImage> images = new ArrayList<>();    // 게시물 이미지 목록

    // 여러 Post는 하나의 User를 가리킴
    // Post.userId 외래키로 User를 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId") // DB에 userId 컬럼 생성
    private User user;    // 게시물 작성자

    // 하나의 Post는 여러 Like 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "post")
    List<Likes> likes = new ArrayList<>();    // 게시물 좋아요 목록
}
