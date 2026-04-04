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

/**
 * 최근 본 게시글 관리를 담당하는 서비스
 * Redis List를 사용하여 최근 본 게시글 목록을 관리
 */
@Service
@RequiredArgsConstructor
public class RecentPostService {
    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;
    /** 최근 본 게시글 최대 개수 */
    private static final int MAX_RECENT = 20;

    /**
     * 최근 본 게시글 추가
     * 중복된 게시글은 제거하고 맨 앞에 추가, 최대 개수 초과 시 오래된 항목 제거
     * @param userId 사용자 ID
     * @param postId 추가할 게시글 ID
     */
    public void addRecentPost(Long userId, Long postId) {
        String key = getKey(userId);
        // 기존에 같은 게시글이 있으면 제거 (중복 방지)
        redisTemplate.opsForList().remove(key, 0, String.valueOf(postId));
        // 맨 앞에 추가 (최신순 유지)
        redisTemplate.opsForList().leftPush(key, String.valueOf(postId));
        // 최대 개수 초과 시 오래된 항목 제거
        redisTemplate.opsForList().trim(key, 0, MAX_RECENT - 1);
    }

    /**
     * 최근 본 게시글 ID 목록 조회
     * @param userId 사용자 ID
     * @return 최근 본 게시글 ID 목록 (최신순)
     */
    public List<Long> getRecentPosts(Long userId) {
        String key = getKey(userId);
        List<String> list = redisTemplate.opsForList().range(key, 0, MAX_RECENT - 1);
        return list.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    /**
     * 최근 본 게시글 전체 정보 조회
     * 게시글 정보, 좋아요 여부, 조회수 포함
     * @param userId 사용자 ID
     * @return 최근 본 게시글 결과 (게시글 목록, 좋아요한 게시글 ID, 조회수)
     */
    public RecentPostsResult getRecentPostsWithDetails(Long userId) {
        // Redis에서 최근 본 게시글 ID 목록 가져오기 (최신순)
        List<Long> ids = getRecentPosts(userId);

        // DB에서 해당 게시글 조회 (N+1 문제 방지를 위해 한 번에 조회)
        Map<Long, PostListDto> postMap = postRepository.findAllActiveByIdIn(ids).stream()
                .map(PostListDto::from)
                .collect(Collectors.toMap(PostListDto::getId, dto -> dto));

        // Redis 순서 유지하면서 게시글 목록 생성 (삭제된 게시글은 필터링)
        List<PostListDto> posts = ids.stream()
                .map(postMap::get)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        // 조회수 조회를 위한 게시글 ID 목록
        List<Long> postIds = posts.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());

        // Redis에서 조회수 가져오기
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        // 사용자가 좋아요한 게시글 ID 목록
        List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
                .filter(like -> like.getPost() != null && !like.getPost().isDeleted())
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());

        return new RecentPostsResult(posts, likedPostIds, redisViewCounts);
    }

    /**
     * Redis 키 생성
     * @param userId 사용자 ID
     * @return Redis 키 (recent:post:{userId})
     */
    private String getKey(Long userId) {
        return "recent:post:" + userId;
    }

    /**
     * 최근 본 게시글 결과를 담는 DTO
     */
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