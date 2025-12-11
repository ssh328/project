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
@Table(indexes = @Index(columnList = "title", name = "제목"))
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private String category;

    @CreationTimestamp
    private LocalDateTime created;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer likeCnt = 0;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ON_SALE'")
    private PostStatus status = PostStatus.ON_SALE;

    // 하나의 Post는 여러 이미지 소유
    @ToString.Exclude
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @BatchSize(size = 50)
    private List<PostImage> images = new ArrayList<>();

    // 여러 Post는 하나의 User를 가리킴
    // Post.userId 외래키로 User를 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId") // DB에 userId 컬럼 생성
    private User user;

    // 하나의 Post는 여러 Like 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "post")
    List<Likes> likes = new ArrayList<>();
}
