package com.song.project.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

import com.song.project.entity.Post;

@Getter
@Setter
public class RecommendedPostDto {
    private Long id;   // 게시물 ID
    private String title;   // 게시물 제목
    private Integer price;   // 게시물 가격
    private String firstImageUrl;   // 게시물 첫 번째 이미지 URL
    private String userId;   // 게시물 작성자 ID
    private LocalDateTime created;   // 게시물 작성일시

    /**
     * Post 엔티티를 RecommendedPostDto로 변환하는 정적 메서드
     * 추천 게시글 목록에 필요한 최소한의 정보만 포함하여 변환
     * 이미지가 있는 경우 첫 번째 이미지 URL을 설정하고, 사용자 정보가 없는 경우 userId는 null로 설정
     * @return RecommendedPostDto 객체 (게시물의 기본 정보와 이미지, 작성자 정보 포함)
     */
    public static RecommendedPostDto from(Post post) {
        RecommendedPostDto dto = new RecommendedPostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());

        // 게시물 이미지 처리 -> 첫 번째 이미지 URL 설정
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            dto.setFirstImageUrl(post.getImages().get(0).getImgUrl());
        } else {
            dto.setFirstImageUrl(null);
        }

        dto.setUserId(post.getUser() != null ? post.getUser().getUsername() : null);
        dto.setCreated(post.getCreated());

        return dto;
    }

    /**
     * 게시물 작성일시로부터 현재까지의 상대적인 시간을 문자열로 반환
     * @return 상대적인 시간을 나타내는 문자열
     */
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
