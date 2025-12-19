package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;    // 사용자 이름

    @Column(name = "dp")
    private String dp;    // 사용자 프로필 이미지

    @Column(name = "email")
    private String email;    // 사용자 이메일

    @Column(name = "role")
    private String role;    // 사용자 역할
}
