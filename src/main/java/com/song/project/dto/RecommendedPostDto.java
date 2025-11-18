package com.song.project.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

import com.song.project.entity.Post;

@Getter
@Setter
public class RecommendedPostDto {
    private Long id;
    private String title;
    private Integer price;
    private String firstImageUrl;
    private String userId;
    private LocalDateTime created;

    // 정적 팩토리 메서드
    public static RecommendedPostDto from(Post post) {
        RecommendedPostDto dto = new RecommendedPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());

        // 이미지 처리
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            dto.setFirstImageUrl(post.getImages().get(0).getImgUrl());
        } else {
            dto.setFirstImageUrl(null);
        }

        dto.setUserId(post.getUser() != null ? post.getUser().getUsername() : null);
        dto.setCreated(post.getCreated());

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
}
