package com.song.project.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.song.project.repository.PostRepository;
import com.song.project.repository.ReviewRepository;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.UserRepository;
import com.song.project.security.JwtUtil;
import com.song.project.dto.PostListDto;
import com.song.project.dto.UserProfileDto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 * 사용자 프로필, 게시물 조회, 비밀번호 관리 등의 기능 제공
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final PostViewCountService postViewCountService;

    /**
     * 사용자가 작성한 게시물 조회
     * @param userId 사용자 ID
     * @param page 페이지 번호 (기본값: 1)
     * @return 게시물 목록, 좋아요 여부, 총 페이지 수, 조회수 정보
     */
    public MyPostResult getMyPosts(Long userId, int page) {
        // 사용자가 작성한 게시물을 최신순으로 조회 (페이지당 20개)
        Page<Post> data = postRepository.findByUserIdOrderByIdDesc(userId, 
            PageRequest.of(page - 1, 20));
        
        Page<PostListDto> postDtos = toPostDtos(data);

        // Redis에서 실시간 조회수 조회
        Map<Long, Long> redisViewCounts = getViewCounts(postDtos);

        // 사용자가 좋아요한 게시물 ID 목록 조회
        List<Long> likedPostIds = getLikedPostIds(userId);

        return new MyPostResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    /**
     * 사용자가 좋아요한 게시물 조회
     * @param userId 사용자 ID
     * @param page 페이지 번호 (기본값: 1)
     * @return 좋아요한 게시물 목록, 좋아요 여부, 총 페이지 수, 조회수 정보
     */
    public MyLikeResult getMyLikes(Long userId, int page) {
        // 사용자가 좋아요한 Likes 엔티티 조회 (페이지당 20개)
        Page<Likes> data = likeRepository.findByUserId(userId, 
            PageRequest.of(page - 1, 20));

        // Likes에서 Post 엔티티 추출
        Page<Post> postPage = data.map(Likes::getPost);

        Page<PostListDto> postDtos = toPostDtos(postPage);

        // Redis에서 실시간 조회수 조회
        Map<Long, Long> redisViewCounts = getViewCounts(postDtos);

        // 사용자가 좋아요한 게시물 ID 목록 조회
        List<Long> likedPostIds = getLikedPostIds(userId);
        
        return new MyLikeResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    /**
     * 사용자 프로필 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 프로필 DTO
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public UserProfileDto getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserProfileDto(user);
    }
    
    /**
     * 사용자 프로필 이미지 업데이트
     * @param userId 사용자 ID
     * @param image 프로필 이미지 URL
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public void updateUserProfileImage(Long userId, String image) {
        User user = getUserOrThrow(userId);
        user.setDp(image);
        userRepository.save(user);
    }

    /**
     * 비밀번호 확인 후 임시 인증 토큰 생성
     * 계정 삭제나 비밀번호 변경 등 중요한 작업 전 인증에 사용
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return 임시 JWT 토큰 (5분 유효)
     * @throws IllegalArgumentException 사용자를 찾을 수 없거나 이메일/비밀번호가 일치하지 않는 경우
     */
    public String verifyPassword(Long userId, String email, String password) {
        User user = getUserOrThrow(userId);
        
        // 이메일과 비밀번호 일치 여부 확인
        if (!user.getEmail().equals(email) || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 임시 토큰 생성 (5분 유효)
        return JwtUtil.createTemporaryToken(
            user.getUsername(), 
            user.getId(), 
            5 * 60 * 1000
        );
    }

    /**
     * 사용자 비밀번호 변경
     * @param userId 사용자 ID
     * @param newPassword 새로운 비밀번호
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public void changePassword(Long userId, String newPassword) {
        User user = getUserOrThrow(userId);

        // 비밀번호를 암호화하여 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 사용자 계정 삭제
     * @param userId 사용자 ID
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public void deleteAccount(Long userId) {
        User user = getUserOrThrow(userId);
        userRepository.delete(user);
    }

    /**
     * 사용자명으로 게시글 목록 조회
     * @param username 사용자명
     * @param page 페이지 번호 (기본값: 1)
     * @return 게시글 페이지 (페이지당 20개)
     */
    public Page<Post> getPostsByUsername(String username, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        return postRepository.findByUser_Username(username, pageRequest);
    }

    /**
     * 여러 게시물에 대한 Redis 조회수 조회
     * @param postIds 게시물 ID 목록
     * @return 게시물 ID를 키로 하는 조회수 맵
     */
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    /**
     * 사용자 프로필 페이지에 필요한 모든 데이터 조회
     * 사용자 정보, 작성한 게시물, 받은 리뷰, 좋아요 여부 등을 포함
     * @param username 조회할 사용자명
     * @param postPage 게시물 페이지 번호 (기본값: 1)
     * @param reviewPage 리뷰 페이지 번호 (기본값: 1)
     * @param loginUserId 현재 로그인한 사용자 ID (null 가능)
     * @return 프로필 페이지에 필요한 모든 데이터
     * @throws IllegalArgumentException 사용자를 찾을 수 없는 경우
     */
    public ProfileResult getProfileResult(String username, int postPage, int reviewPage, Long loginUserId) {

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        UserProfileDto userDto = new UserProfileDto(user);

        // 사용자가 작성한 게시물 조회 및 DTO 변환
        Page<Post> posts = getPostsByUsername(username, postPage);
        Page<PostListDto> postDtos = posts.map(PostListDto::from);

        // 게시물 ID 목록 추출 후 Redis에서 조회수 조회
        List<Long> postIds = postDtos.stream()
            .map(PostListDto::getId)
            .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);

        // 사용자가 받은 리뷰 조회 (최신순, 페이지당 3개)
        PageRequest reviewPageRequest = PageRequest.of(
            reviewPage - 1,
            3,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Review> reviews = reviewRepository.findByTargetUser_Id(
                userDto.getId(), reviewPageRequest);

        // 로그인한 사용자가 좋아요한 게시물 ID 목록 조회
        List<Long> likedPostIds = getLikedPostIds(user.getId());

        return new ProfileResult(userDto,
                                 postDtos, 
                                 reviews, 
                                 likedPostIds, 
                                 posts.getTotalPages(), 
                                 reviews.getTotalPages(), 
                                 loginUserId, 
                                 viewCounts);
    }

    // ===========================
    // 헬퍼 메서드
    // ===========================

    // 사용자 조회, 없으면 예외 발생
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    // Post 엔티티 페이지를 PostListDto 페이지로 변환
    private Page<PostListDto> toPostDtos(Page<Post> posts) {
        return posts.map(PostListDto::from);
    }

    /**
     * PostListDto 페이지에서 게시물 ID 목록을 추출하고 Redis에서 조회수를 조회
     * @param postDtos PostListDto 페이지
     * @return 게시물 ID를 키로 하는 조회수 맵
     */
    private Map<Long, Long> getViewCounts(Page<PostListDto> postDtos) {
        // DTO에서 게시물 ID 목록 추출
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());
        // Redis에서 실시간 조회수 조회
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    /**
     * 사용자가 좋아요한 게시물 ID 목록을 조회
     * @param userId 사용자 ID
     * @return 좋아요한 게시물 ID 목록
     */
    private List<Long> getLikedPostIds(Long userId) {
        return likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());
    }

    // DTO 클래스

    /**
     * 내가 작성한 게시물 조회 결과 DTO
     */
    @Getter
    public static class MyPostResult {
        private final Page<PostListDto> posts;
        private final List<Long> likedPostIds;
        private final int totalPages;
        private final Map<Long, Long> viewCounts;

        public MyPostResult(Page<PostListDto> posts, List<Long> likedPostIds, 
                            int totalPages, Map<Long, Long> viewCounts) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
            this.totalPages = totalPages;
            this.viewCounts = viewCounts;
        }
    }

    /**
     * 내가 좋아요한 게시물 조회 결과 DTO
     */
    @Getter
    public static class MyLikeResult {
        private final Page<PostListDto> posts;
        private final List<Long> likedPostIds;
        private final int totalPages;
        private final Map<Long, Long> viewCounts;

        public MyLikeResult(Page<PostListDto> posts, List<Long> likedPostIds, 
                            int totalPages, Map<Long, Long> viewCounts) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
            this.totalPages = totalPages;
            this.viewCounts = viewCounts;
        }
    }

    /**
     * 사용자 프로필 페이지 조회 결과 DTO
     * 사용자 정보, 게시물, 리뷰, 좋아요 여부 등을 포함
     */
    @Getter
    public static class ProfileResult {
        private UserProfileDto user;
        private Page<PostListDto> posts;
        private Page<Review> reviews;
        private List<Long> likedPostIds;
        private int postTotalPages;
        private int reviewTotalPages;
        private Long loginUserId;
        private Map<Long, Long> viewCounts;

        public ProfileResult(UserProfileDto user, 
                             Page<PostListDto> posts, 
                             Page<Review> reviews, 
                             List<Long> likedPostIds, 
                             int postTotalPages, 
                             int reviewTotalPages, 
                             Long loginUserId, 
                             Map<Long, Long> viewCounts) {
            this.user = user;
            this.posts = posts;
            this.reviews = reviews;
            this.likedPostIds = likedPostIds;
            this.postTotalPages = postTotalPages;
            this.reviewTotalPages = reviewTotalPages;
            this.loginUserId = loginUserId;
            this.viewCounts = viewCounts;
        }
    }
}