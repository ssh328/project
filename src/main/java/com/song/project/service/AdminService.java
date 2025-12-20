package com.song.project.service;

import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.entity.PostStatus;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 관리자 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final S3Service s3Service;

    /**
     * 대시보드 통계 정보 조회
     * @return 사용자 수, 게시글 수, 리뷰 수, 좋아요 수를 포함한 통계 정보
     */
    public DashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalReviews = reviewRepository.count();
        long totalLikes = likeRepository.count();

        return new DashboardStats(totalUsers, totalPosts, totalReviews, totalLikes);
    }

    /**
     * 사용자 목록을 검색 및 페이지네이션과 함께 조회
     * @param keyword 검색 키워드 (사용자명 또는 이메일, 선택적)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기
     * @return 사용자 페이지
     */
    public Page<User> getUsers(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(pageRequest);
        }

        return userRepository.fullTextSearchUsernameOrEmail(keyword, pageRequest);
    }

    /**
     * 게시글 목록을 검색, 필터링 및 페이지네이션과 함께 조회
     * @param keyword 검색 키워드 (게시글 제목, 선택적)
     * @param category 카테고리 필터 (선택적)
     * @param status 게시글 상태 필터 (선택적)
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기
     * @return 게시글 페이지
     */
    public Page<Post> getPosts(String keyword, String category, PostStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "created"));

        if (keyword != null && !keyword.isBlank()) {
            // 제목 기준 간단 검색 (full text 보다 단순 JPA 메서드 사용)
            return postRepository.findByTitleContainingIgnoreCase(keyword, pageRequest);
        }

        // 기존 필터 메서드를 활용 (가격 조건은 사용하지 않으므로 null)
        return postRepository.findWithFilter(category, null, null, status, pageRequest);
    }

    /**
     * 리뷰 목록을 페이지네이션과 함께 조회
     * @param page 페이지 번호 (기본값: 1)
     * @param size 페이지 크기
     * @return 리뷰 페이지
     */
    public Page<Review> getReviews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findAll(pageRequest);
    }

    /**
     * 관리자 권한으로 게시글 삭제
     * 작성자와 무관하게 삭제 가능, S3에 저장된 이미지도 함께 삭제
     * @param postId 삭제할 게시글 ID
     * @throws IllegalArgumentException 게시글을 찾을 수 없는 경우
     */
    public void deletePostAsAdmin(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // S3 이미지 삭제
        List<PostImage> images = post.getImages();
        if (images != null) {
            for (PostImage img : images) {
                String key = s3Service.extractS3Key(img.getImgUrl());
                s3Service.deleteFile(key);
            }
        }

        postRepository.delete(post);
    }

    /**
     * 관리자 권한으로 리뷰 삭제
     * @param reviewId 삭제할 리뷰 ID
     * @throws IllegalArgumentException 리뷰를 찾을 수 없는 경우
     */
    public void deleteReviewAsAdmin(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다.");
        }
        reviewRepository.deleteById(reviewId);
    }

    /**
     * 대시보드 통계 정보를 담는 DTO
     */
    @Getter
    public static class DashboardStats {
        private final long totalUsers;
        private final long totalPosts;
        private final long totalReviews;
        private final long totalLikes;

        public DashboardStats(long totalUsers,
                              long totalPosts,
                              long totalReviews,
                              long totalLikes) {
            this.totalUsers = totalUsers;
            this.totalPosts = totalPosts;
            this.totalReviews = totalReviews;
            this.totalLikes = totalLikes;
        }
    }
}