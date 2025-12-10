package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Likes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 여러 Likes가 같은 User를 가리킬 수 있음
    // Like.userId 외래키로 User 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    // 여러 Likes가 같은 Post를 가리킬 수 있음
    // Like.postId 외래키로 Post 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;
}