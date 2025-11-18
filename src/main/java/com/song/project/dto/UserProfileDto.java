package com.song.project.dto;
import com.song.project.entity.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileDto {
    private Long id;
    private String username;
    private String dp;
    private String email;

    public UserProfileDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.dp = user.getDp();
        this.email = user.getEmail();
    }
}
