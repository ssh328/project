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
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String user_id;
    private String email;
    private String password;
    private String dp;
    private String role;

    // 하나의 User가 여러 Like 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    List<Likes> likes = new ArrayList<>();

    // 하나의 User가 여러 Post 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    List<Post> posts = new ArrayList<>();
}
