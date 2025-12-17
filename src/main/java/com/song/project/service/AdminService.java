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

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final S3Service s3Service;

    // 대시보드 통계
    public DashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalReviews = reviewRepository.count();
        long totalLikes = likeRepository.count();

        return new DashboardStats(totalUsers, totalPosts, totalReviews, totalLikes);
    }

    // 사용자 목록 (검색 + 페이징)
    public Page<User> getUsers(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(pageRequest);
        }

        return userRepository.fullTextSearchUsernameOrEmail(keyword, pageRequest);
    }

    // 게시글 목록 (검색 + 상태 필터 + 페이징)
    public Page<Post> getPosts(String keyword, String category, PostStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "created"));

        if (keyword != null && !keyword.isBlank()) {
            // 제목 기준 간단 검색 (full text 보다 단순 JPA 메서드 사용)
            return postRepository.findByTitleContainingIgnoreCase(keyword, pageRequest);
        }

        // 기존 필터 메서드를 활용 (가격 조건은 사용하지 않으므로 null)
        return postRepository.findWithFilter(category, null, null, status, pageRequest);
    }

    // 리뷰 목록 (페이징)
    public Page<Review> getReviews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepository.findAll(pageRequest);
    }

    // 게시글 삭제 (관리자용 - 작성자와 무관하게 삭제 가능)
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

    // 리뷰 삭제 (관리자용)
    public void deleteReviewAsAdmin(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다.");
        }
        reviewRepository.deleteById(reviewId);
    }

    // DTO
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