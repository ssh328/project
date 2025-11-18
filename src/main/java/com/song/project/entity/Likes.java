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

    // @ManyToOne사용할 때 이런 fetchType설정도 넣어줄 수 있는데
    // - FetchType.EAGER를 넣으면 "이거 필요없어도 다른 테이블 항상 가져와주세요" 라는 뜻이고
    // - FetchType.LAZY를 넣으면 "게으르게 필요할 때만 가져와주세요" 라는 뜻입니다.

    // Like 하나는 한 명의 User 누른것
    // 여러 Likes가 같은 User를 가리킬 수 있음
    // Many(Likes) -> One(User)
    // Like.userId 외래키로 User 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    // Like 하나는 “한 개의 Post”에 눌린 것
    // 여러 Likes가 같은 Post를 가리킬 수 있음
    // Many(Likes) → One(Post)
    // Like.postId 외래키로 Post 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;
}