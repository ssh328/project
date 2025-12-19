package com.song.project.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.song.project.entity.PostImage;
import com.song.project.entity.PostStatus;
import com.song.project.entity.Post;

@Getter
@Setter
public class PostEditDto {
    private Long id;   // 게시물 ID
    private String title;   // 게시물 제목
    private String body;   // 게시물 내용
    private Integer price;   // 게시물 가격
    private String category;   // 게시물 카테고리
    private LocalDateTime created;   // 게시물 작성일시
    private Integer like_cnt;   // 게시물 좋아요 수
    private PostStatus status;   // 게시물 상태 (ON_SALE, SOLD_OUT, RESERVED)
    private List<PostImage> images;   // 게시물 이미지 목록
    private String userId;   // 게시물 작성자 ID

    /**
     * Post 엔티티를 PostEditDto로 변환하는 정적 메서드
     * 엔티티의 모든 필드를 DTO로 복사하며, 사용자 정보가 없는 경우 userId는 null로 설정
     * @return PostEditDto 객체 (엔티티의 모든 필드가 복사됨)
     */
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

    /**
     * 게시물 상태의 설명 문자열을 반환
     * 상태가 null인 경우 빈 문자열을 반환
     * @return 게시물 상태 설명 문자열 (예: "판매중", "예약중", "판매완료")
     */
    public String getStatusDescription() {
        return status != null ? status.getDescription() : "";
    }
}