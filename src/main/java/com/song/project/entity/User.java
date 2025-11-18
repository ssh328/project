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
    // private String name; // Chat.java에서 옮김
    private String dp;   // Chat.java에서 옮김
    private String role; // Chat.java에서 옮김

    // 하나의 User가 여러 Like 가리킴
    @ToString.Exclude
    // 컬럼 하나 만든 다음 타입을 List<다른테이블>로 집어넣고
    // 그리고 @OneToMany(mappedBy = "내 컬럼 훔쳐쓰고있는 다른 컬럼명") 이걸 붙여줍니다.
    // 그럼 내 id를 훔쳐서 쓰고 있는 테이블의 행들을 전부 출력해줍니다.
    @OneToMany(mappedBy = "user")
    List<Likes> likes = new ArrayList<>();

    // 하나의 User가 여러 Post 가리킴
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    List<Post> posts = new ArrayList<>();
}
