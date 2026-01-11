package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(indexes = {
        @Index(columnList = "username", name = "idx_user_username"),
        @Index(columnList = "user_id", name = "idx_user_user_id"),
        @Index(columnList = "email", name = "idx_user_email")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;    // 사용자 이름

    private String user_id;    // 사용자 아이디

    private String email;    // 사용자 이메일

    private String password;    // 사용자 비밀번호

    private String dp;    // 사용자 프로필 이미지

    private String role;    // 사용자 역할

    // 하나의 User가 여러 Like 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    List<Likes> likes = new ArrayList<>();    // 사용자 좋아요 목록

    // 하나의 User가 여러 Post 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    List<Post> posts = new ArrayList<>();    // 사용자 게시물 목록
}
