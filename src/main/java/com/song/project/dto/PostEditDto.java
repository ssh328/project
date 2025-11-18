package com.song.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.song.project.post.PostStatus;
import com.song.project.entity.PostImage;
import com.song.project.entity.Post;


@Getter
@Setter
public class PostEditDto {
    private Long id;
    private String title;
    private String body;
    private Integer price;
    private String category;
    private LocalDateTime created;
    private Integer like_cnt;
    private PostStatus status;
    private List<PostImage> images;
    private String userId; // 사용자 ID만 필요하므로 User 객체 대신 String으로

    // Post 엔티티를 PostEditDto로 변환하는 정적 메서드
    public static PostEditDto from(Post post) {
        PostEditDto dto = new PostEditDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setBody(post.getBody());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCreated(post.getCreated());
        dto.setLike_cnt(post.getLikeCnt());
        dto.setStatus(post.getStatus());
        dto.setImages(post.getImages());
        dto.setUserId(post.getUser() != null ? post.getUser().getUsername() : null);
        return dto;
    }

    public String getRelativeTime() {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(created, now);

        long seconds = duration.getSeconds();
        if (seconds < 60) return "방금 전";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "분 전";
        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";
        long days = hours / 24;
        if (days < 7) return days + "일 전";
        long weeks = days / 7;
        if (weeks < 5) return weeks + "주 전";
        long months = days / 30;
        if (months < 12) return months + "개월 전";
        long years = days / 365;
        return years + "년 전";
    }

    public String getStatusDescription() {
        return status != null ? status.getDescription() : "";
    }
}