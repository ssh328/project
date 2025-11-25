package com.song.project.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.song.project.repository.PostRepository;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;
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
    private final PostViewCountService postViewCountService;

    // 회원가입 처리
    public RegisterResult register(String userId, String password, String username, String email) {
        User user = new User();
        user.setUser_id(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setDp("https://javaspringproject.s3.ap-northeast-2.amazonaws.com/project/default-profile-img.png");

        User savedUser = userRepository.save(user);
        return new RegisterResult(savedUser);
    }

    // 사용자가 작성한 게시물 조회
    public MyPostResult getMyPosts(Long userId, int page) {
        Page<Post> data = postRepository.findByUserIdOrderByIdDesc(userId, 
            PageRequest.of(page - 1, 20));
        
        Page<PostListDto> postDtos = data.map(PostListDto::from);

        List<Long> postIds = postDtos.stream()
            .map(PostListDto::getId)
            .collect(Collectors.toList());
        
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
            .map(like -> like.getPost().getId())
            .collect(Collectors.toList());

        return new MyPostResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    // 사용자가 좋아요한 게시물 조회
    public MyLikeResult getMyLikes(Long userId, int page) {
        Page<Likes> data = likeRepository.findByUserId(userId, 
            PageRequest.of(page - 1, 20));
        Page<Post> postPage = data.map(Likes::getPost);
        Page<PostListDto> postDtos = postPage.map(PostListDto::from);

        List<Long> postIds = postDtos.stream()
            .map(PostListDto::getId)
            .collect(Collectors.toList());

        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
            .map(like -> like.getPost().getId())
            .collect(Collectors.toList());
        
        return new MyLikeResult(postDtos, likedPostIds, data.getTotalPages(), redisViewCounts);
    }

    // 사용자 정보 조회
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
            return new UserProfileDto(user);
    }
    
    // 사용자 프로필 이미지 업데이트
    public void updateUserProfileImage(Long userId, String image) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setDp(image);
        userRepository.save(user);
    }

    // 비밀번호 확인 후 인증 토큰 생성
    public String verifyPassword(Long userId, String email, String password) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!user.getEmail().equals(email) || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return JwtUtil.createTemporaryToken(
            user.getUsername(), 
            user.getId(), 
            5 * 60 * 1000
        );
    }

    // 비밀번호 변경
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // 계정 삭제
    public void deleteAccount(Long userId) {

        // 이 부분 따로 함수 만들기. 재사용을 많이함
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
            userRepository.delete(user); 
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
}