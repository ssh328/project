package com.song.project.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.song.project.entity.Post;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 게시글 추천 관련 비즈니스 로직을 처리하는 서비스
 * Redis Sorted Set을 사용하여 인기 게시글 점수를 관리하고 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendedPostService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    /** Redis Sorted Set 키: popular:posts */
    private static final String POPULAR_KEY = "popular:posts";

    /** 조회수 가중치 */
    private static final double VIEW_WEIGHT = 1.0;
    /** 좋아요 가중치 (조회수보다 3배 높음) */
    private static final double LIKE_WEIGHT = 3.0;

    /** 추천 게시글 최대 개수 */
    private static final int RECOMMEND_LIMIT = 5;

    /**
     * 게시글 조회 시 인기 점수 증가
     * @param postId 게시글 ID
     */
    public void addViewScore(Long postId) {
        incrementScore(postId, VIEW_WEIGHT);
    }

    /**
     * 게시글 좋아요 추가/삭제 시 인기 점수 업데이트
     * 현재 좋아요 수를 기준으로 점수를 재계산하여 설정
     * @param postId 게시글 ID
     */
    public void addLikeScore(Long postId) {
        int likeCount = likeRepository.findByPostId(postId).size();
        double score = likeCount * LIKE_WEIGHT;
        redisTemplate.opsForZSet().add(POPULAR_KEY, postId.toString(), score);
    }

    /**
     * Redis Sorted Set에 인기 점수 누적
     * @param postId 게시글 ID
     * @param weight 증가시킬 점수
     */
    public void incrementScore(Long postId, double weight) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, postId.toString(), weight);
    }

    /**
     * 인기 게시글 Top N 조회
     * @param limit 조회할 게시글 개수
     * @return 인기 게시글 ID 목록 (점수 높은 순)
     */
    public List<Long> getTopPopularPosts(int limit) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, limit - 1);
        if (set == null) return Collections.emptyList();
        return set.stream()
                .map(tuple -> Long.valueOf(tuple.getValue().toString()))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 기반 인기 게시글 추천
     * 현재 게시글과 같은 카테고리 내에서 Redis 점수 기준으로 인기 게시글 Top N 반환
     * @param currentPost 현재 게시글
     * @return 추천 게시글 목록 (최대 5개, 점수 높은 순)
     */
    public List<Post> recommendPopularByCategory(Post currentPost) {
       
        if (currentPost == null) return Collections.emptyList();
        String category = currentPost.getCategory();

        // Redis Sorted Set에서 전체 인기 게시글 가져오기 (점수 높은 순)
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, -1);

        if (set == null || set.isEmpty()) return Collections.emptyList();

        // 현재 게시글 제외하고 모든 postId 수집 (점수 순서 유지)
        List<Long> allPostIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : set) {
            Long postId = Long.valueOf(tuple.getValue().toString());
            if (!postId.equals(currentPost.getId())) {
                allPostIds.add(postId);
                log.debug("추천 게시글 후보: postId={}, score={}", postId, tuple.getScore());
            }
        }

        if (allPostIds.isEmpty()) return Collections.emptyList();

        // N+1 문제 방지: 모든 게시글을 한 번에 조회
        List<Post> posts = postRepository.findAllActiveByIdIn(allPostIds);

        Map<Long, Post> postMap = posts.stream()
        .collect(Collectors.toMap(Post::getId, post -> post));

        // 카테고리 필터 적용 (Redis 점수 순서 유지하면서)
        List<Post> result = new ArrayList<>();
        for (Long postId : allPostIds) {
            Post post = postMap.get(postId);
            if (post != null && category.equals(post.getCategory())) {
                result.add(post);
                // 최대 개수 도달 시 중단
                if (result.size() >= RECOMMEND_LIMIT) break;
            }
        }

        return result;
    }
}
