package com.song.project.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    private int rating;
    
    // 여러 Review는 하나의 User(작성자)를 가리킴
    // Many(Review) -> One(User)
    // Review.reviewer 외래키로 User 참조
    @ManyToOne
    @JoinColumn(name = "reviewer",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User reviewer;

    // 여러 Review는 하나의 User(대상자)를 가리킴
    // Many(Review) -> One(User)
    // Review.targetUser 외래키로 User 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User targetUser;

    @CreationTimestamp
    private LocalDateTime createdAt;
}