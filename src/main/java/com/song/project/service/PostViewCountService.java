package com.song.project.service;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.song.project.entity.Post;
import com.song.project.repository.PostRepository;

/**
 * 게시글 조회수 관리를 담당하는 서비스
 * Redis를 사용하여 실시간 조회수를 관리하고, 주기적으로 MySQL에 동기화
 */
@Service
@RequiredArgsConstructor
public class PostViewCountService {
    private final PostRepository postRepository;
    private final RedisTemplate<String, String> redisTemplate;
    /** Redis 키 접두사: post:view:{postId} */
    private static final String VIEW_PREFIX = "post:view:";
    /** 조회 기록 TTL: 1시간 (중복 조회 방지) */
    private static final long VIEW_TTL_SECONDS = 60 * 60;

    /**
     * 여러 게시물의 조회수를 Redis에서 조회
     * @param postIds 게시물 ID 목록
     * @return 게시물 ID와 조회수를 담은 Map (조회수가 없으면 0)
     */
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        Map<Long, Long> result = new HashMap<>();

        // 각 게시물의 Redis 조회수 조회
        for (Long postId : postIds) {
            String key = VIEW_PREFIX + postId;
            String value = redisTemplate.opsForValue().get(key);

            Long redisCount = (value != null) ? Long.parseLong(value) : 0L;
            result.put(postId, redisCount);
        }

        return result;
    }

    /**
     * 게시글 조회수 증가 및 현재 조회수 반환
     * 중복 조회 방지: 로그인 사용자는 userId, 비로그인 사용자는 viewToken으로 구분
     * @param postId 게시물 ID
     * @param userId 로그인한 사용자 ID (선택적)
     * @param viewToken 비로그인 사용자용 조회 토큰 (선택적)
     * @return 증가된 조회수 (이미 조회한 경우 현재 조회수만 반환)
     */
    public Long incrementAndGetViewCount(Long postId, Long userId, String viewToken) {

        String uniqueViewerKey;

        // 로그인 사용자: post:view:{postId}:user:{userId}
        if (userId != null) {
            uniqueViewerKey = VIEW_PREFIX + postId + ":user:" + userId;
        } 
        // 비로그인 사용자: post:view:{postId}:guest:{viewToken}
        else {
            uniqueViewerKey = VIEW_PREFIX + postId + ":guest:" + viewToken;
        }

        // 중복 조회 방지: 일정 시간(1시간) 내 조회했는지 확인
        Boolean hasViewed = redisTemplate.hasKey(uniqueViewerKey);
        if (hasViewed != null && hasViewed) {
            // 이미 조회한 경우, 조회수 증가하지 않고 현재 조회수만 반환
            return getViewCount(postId);
        }

        // 조회수 증가 (Redis INCR 연산)
        String key = VIEW_PREFIX + postId;
        Long viewCount = redisTemplate.opsForValue().increment(key, 1);

        // 조회 기록 저장 (1시간 TTL로 중복 조회 방지)
        redisTemplate.opsForValue().set(uniqueViewerKey, "1", Duration.ofSeconds(VIEW_TTL_SECONDS));

        return viewCount;
    }

    /**
     * 게시글의 현재 조회수 조회 (Redis에서)
     * @param postId 게시물 ID
     * @return 현재 조회수 (조회수가 없으면 0)
     */
    public Long getViewCount(Long postId) {
        String key = VIEW_PREFIX + postId;
        Object value = redisTemplate.opsForValue().get(key);
        return (value != null) ? Long.parseLong(value.toString()) : 0L;
    }

    /**
     * Redis의 조회수를 MySQL에 주기적으로 동기화
     * 10분마다 자동 실행되며, Redis의 조회수를 DB에 반영
     */
    // @Scheduled(fixedRate = 10 * 60 * 1000) // 10분마다 실행
    public void flushViewCountsToDB() {
        // Redis에 있는 모든 조회수 키 가져오기 (post:view:* 형태)
        Set<String> keys = redisTemplate.keys(VIEW_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return;

        // postId를 키로, Redis 조회수를 값으로 저장
        Map<Long, Long> postIdToRedisCount = new HashMap<>();

        // Redis 키 순회: 조회수 키만 추출 (유저 조회 기록 키는 제외)
        for (String key : keys) {
            String postIdStr = key.replace(VIEW_PREFIX, "");
            // 유저 조회 기록 키는 제외 (post:view:{postId}:user:{userId} 또는 :guest:{token})
            if (postIdStr.contains(":user:") || postIdStr.contains(":guest:")) continue;

            // 순수 조회수 키만 처리 (post:view:{postId})
            Long postId = Long.parseLong(postIdStr);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && !value.isEmpty()) {
                postIdToRedisCount.put(postId, Long.parseLong(value));
            }
        }

        if (postIdToRedisCount.isEmpty()) return;

        // N+1 문제 방지: 모든 게시물을 한 번에 조회
        List<Long> postIds = new ArrayList<>(postIdToRedisCount.keySet());
        Map<Long, Post> postMap = postRepository.findAllById(postIds)
                .stream()
                .collect(Collectors.toMap(Post::getId, post -> post));

        // Redis 조회수를 DB 조회수에 반영
        for (Map.Entry<Long, Long> entry : postIdToRedisCount.entrySet()) {
            Long postId = entry.getKey();
            Long redisCount = entry.getValue();
            Post post = postMap.get(postId);
            if (post == null) continue;
            
            Long dbCount = post.getViewCount();
            Long updatedCount;

            // 조회수 계산 로직
            // DB 조회수가 0이면 Redis 값을 그대로 사용
            // DB 조회수가 있으면 Redis와의 차이만큼 더하기 (증분 업데이트)
            if (dbCount == 0) {
                updatedCount = redisCount;
            } else {
                // Redis 값이 DB 값보다 큰 경우에만 업데이트 (차이만큼 증가)
                updatedCount = dbCount + (redisCount - dbCount);
            }

            post.setViewCount(updatedCount);
            postRepository.save(post);
        }
    }
}