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
import com.song.project.exception.BadRequestException;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.UserRepository;
import com.song.project.JwtUtil;
import com.song.project.dto.PostListDto;
import com.song.project.dto.UserProfileDto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final PostViewCountService postViewCountService;

    // 회원가입 처리
    public RegisterResult register(String userId, String password, String username, String email) {

        if (userRepository.findByUser_id(userId).isPresent()) {
            throw new BadRequestException("이미 존재하는 아이디입니다.");
        }

        if (userId.length() < 4 || userId.length() > 20) {
            throw new BadRequestException("아이디는 4자 이상 20자 이하여야 합니다.");
        }
        // 영문, 숫자만 허용
        if (!userId.matches("^[a-zA-Z0-9]+$")) {
            throw new BadRequestException("아이디는 영문과 숫자만 사용할 수 있습니다.");
        }

        if (password.length() < 8 || password.length() > 20) {
            throw new BadRequestException("비밀번호는 8자 이상 20자 이하여야 합니다.");
        }
        // 최소 1개 이상의 영문, 숫자 포함
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).+$")) {
            throw new BadRequestException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }

        User user = new User();
        user.setUser_id(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("default");
        user.setDp("https://javaspringproject.s3.ap-northeast-2.amazonaws.com/project/default-profile-img.png");

        User savedUser = userRepository.save(user);
        return new RegisterResult(savedUser);
    }

    // 사용자가 작성한 게시물 조회
    public MyPostResult getMyPosts(Long userId, int page) {
        Page<Post> data = postRepository.findByUserIdOrderByIdDesc(userId, 
            PageRequest.of(page - 1, 20));
        
        Page<PostListDto> postDtos = toPostDtos(data);
        Map<Long, Long> redisViewCounts = getViewCounts(postDtos);
        List<Long> likedPostIds = getLikedPostIds(userId);

        return new MyPostResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    // 사용자가 좋아요한 게시물 조회
    public MyLikeResult getMyLikes(Long userId, int page) {
        Page<Likes> data = likeRepository.findByUserId(userId, 
            PageRequest.of(page - 1, 20));
        Page<Post> postPage = data.map(Likes::getPost);
        Page<PostListDto> postDtos = toPostDtos(postPage);

        Map<Long, Long> redisViewCounts = getViewCounts(postDtos);
        List<Long> likedPostIds = getLikedPostIds(userId);
        
        return new MyLikeResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    // 사용자 정보 조회
    public UserProfileDto getUserProfile(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserProfileDto(user);
    }
    
    // 사용자 프로필 이미지 업데이트
    public void updateUserProfileImage(Long userId, String image) {
        User user = getUserOrThrow(userId);
        user.setDp(image);
        userRepository.save(user);
    }

    // 비밀번호 확인 후 인증 토큰 생성
    public String verifyPassword(Long userId, String email, String password) {
        User user = getUserOrThrow(userId);
        
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

    // 비밀번호 변경
    public void changePassword(Long userId, String newPassword) {
        User user = getUserOrThrow(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 계정 삭제
    public void deleteAccount(Long userId) {
        User user = getUserOrThrow(userId);
        userRepository.delete(user);
    }

    // 사용자명으로 게시글 목록 조회
    public Page<Post> getPostsByUsername(String username, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        return postRepository.findByUser_Username(username, pageRequest);
    }

    // 여러 게시물에 대한 Redis 조회수 조회
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    public ProfileResult getProfileResult(String username, int postPage, int reviewPage, Long loginUserId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        UserProfileDto userDto = new UserProfileDto(user);

        Page<Post> posts = getPostsByUsername(username, postPage);
        Page<PostListDto> postDtos = posts.map(PostListDto::from);

        List<Long> postIds = postDtos.stream()
            .map(PostListDto::getId)
            .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);

        PageRequest reviewPageRequest = PageRequest.of(
            reviewPage - 1,
            3,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Review> reviews = reviewRepository.findByTargetUser_Id(
                userDto.getId(), reviewPageRequest);

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

    // Private Helper Methods

    // 사용자 조회, 없으면 예외 발생
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    // Post 엔티티 페이지를 PostListDto 페이지로 변환
    private Page<PostListDto> toPostDtos(Page<Post> posts) {
        return posts.map(PostListDto::from);
    }

    // PostListDto 페이지에서 게시물 ID 목록을 추출하고 Redis에서 조회수를 조회
    private Map<Long, Long> getViewCounts(Page<PostListDto> postDtos) {
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    // 사용자가 좋아요한 게시물 ID 목록을 조회
    private List<Long> getLikedPostIds(Long userId) {
        return likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());
    }

    // DTO 클래스
    @Getter
    public static class RegisterResult {
        private final Long id;
        private final String username;
        private final String email;

        public RegisterResult(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
        }
    }

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