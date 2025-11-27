package com.song.project.service;

import com.song.project.dto.PostListDto;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentPostService {
    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;
    private static final int MAX_RECENT = 20;

    // 최근 본 상품 추가 (중복 제거)
    public void addRecentPost(Long userId, Long postId) {
        String key = getKey(userId);
        redisTemplate.opsForList().remove(key, 0, String.valueOf(postId));
        redisTemplate.opsForList().leftPush(key, String.valueOf(postId));
        redisTemplate.opsForList().trim(key, 0, MAX_RECENT - 1);
    }

    // 최근 본 상품 목록 조회
    public List<Long> getRecentPosts(Long userId) {
        String key = getKey(userId);
        List<String> list = redisTemplate.opsForList().range(key, 0, MAX_RECENT - 1);
        return list.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    // 최근 본 게시글 전체 정보 조회 (게시글, 좋아요, 조회수 포함)
    public RecentPostsResult getRecentPostsWithDetails(Long userId) {
        // Redis에서 최근 본 상품 ID 목록 가져오기
        List<Long> ids = getRecentPosts(userId);

        // DB에서 해당 게시글 조회 및 순서 유지
        Map<Long, PostListDto> postMap = postRepository.findAllById(ids).stream()
                .map(PostListDto::from)
                .collect(Collectors.toMap(PostListDto::getId, dto -> dto));

        List<PostListDto> posts = ids.stream()
                .map(postMap::get)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        // 조회수 합산용 처리
        List<Long> postIds = posts.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());

        // Redis 조회수 가져오기
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        // 좋아요한 게시글 ID 목록
        List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());

        return new RecentPostsResult(posts, likedPostIds, redisViewCounts);
    }

    private String getKey(Long userId) {
        return "recent:post:" + userId;
    }

    @Getter
    public static class RecentPostsResult {
        private List<PostListDto> posts;
        private List<Long> likedPostIds;
        private Map<Long, Long> viewCounts;

        public RecentPostsResult(List<PostListDto> posts, List<Long> likedPostIds, 
                            Map<Long, Long> viewCounts) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
            this.viewCounts = viewCounts;
        }
    }
}