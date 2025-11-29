package com.song.project.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.song.project.entity.Post;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendedPostService {
    private final RedisTemplate<String, String> redisTemplate;
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    private static final String POPULAR_KEY = "popular:posts";

    // 가중치 설정
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 3.0;

    private static final int RECOMMEND_LIMIT = 5;

    // 게시글 조회 시 호출
    public void addViewScore(Long postId) {
        incrementScore(postId, VIEW_WEIGHT);
    }

    // 게시글 좋아요 추가/삭제 시 호출
    public void addLikeScore(Long postId) {
        int likeCount = likeRepository.findByPostId(postId).size();
        double score = likeCount * LIKE_WEIGHT;
        redisTemplate.opsForZSet().add(POPULAR_KEY, postId.toString(), score);
    }

    // 실제 ZSet에 점수 누적
    public void incrementScore(Long postId, double weight) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, postId.toString(), weight);
    }

    public List<Long> getTopPopularPosts(int limit) {
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, limit - 1);
        if (set == null) return Collections.emptyList();
        return set.stream()
                .map(tuple -> Long.valueOf(tuple.getValue().toString()))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 기반 인기 게시글 추천 (Post 객체 반환)
     * 최근 본 게시물 리스트를 기반으로 카테고리를 선택
     * 선택한 카테고리 내에서 Redis에 저장된 점수 기준으로 인기 게시글 Top N 반환
     */
    public List<Post> recommendPopularByCategory(Post currentPost) {
       
        if (currentPost == null) return Collections.emptyList();
        String category = currentPost.getCategory();

        // Redis에서 전체 인기 게시글 가져오기
        Set<ZSetOperations.TypedTuple<String>> set = redisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_KEY, 0, -1); // 전체 인기 게시글

        if (set == null || set.isEmpty()) return Collections.emptyList();

        // 모든 postId를 먼저 수집
        List<Long> allPostIds = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : set) {
            System.out.println("postId=" + tuple.getValue() + ", score=" + tuple.getScore());
            Long postId = Long.valueOf(tuple.getValue().toString());
            if (!postId.equals(currentPost.getId())) {
                allPostIds.add(postId);
            }
        }

        if (allPostIds.isEmpty()) return Collections.emptyList();

        // 한 번에 조회 (EntityGraph로 images, user 함께 로드)
        List<Post> posts = postRepository.findAllById(allPostIds);

        // 카테고리 필터 적용
        // List<Post> result = new ArrayList<>();
        // for (Post post : posts) {
        //     if (category.equals(post.getCategory())) {
        //         result.add(post);
        //         if (result.size() >= RECOMMEND_LIMIT) break;
        //     }
        // }

        Map<Long, Post> postMap = posts.stream()
        .collect(Collectors.toMap(Post::getId, post -> post));

        // 카테고리 필터 적용 (Redis 순서 유지)
        List<Post> result = new ArrayList<>();
        for (Long postId : allPostIds) {
            Post post = postMap.get(postId);
            if (post != null && category.equals(post.getCategory())) {
                result.add(post);
                if (result.size() >= RECOMMEND_LIMIT) break;
            }
        }

        return result;
    }
}
