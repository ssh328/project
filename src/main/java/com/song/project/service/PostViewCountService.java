package com.song.project.service;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.song.project.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class PostViewCountService {
    private final PostRepository postRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String VIEW_PREFIX = "post:view:"; // key: post:view:{postId}
    private static final long VIEW_TTL_SECONDS = 60 * 60;

    // 여러 게시물의 조회수를 가져오는 함수
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        Map<Long, Long> result = new HashMap<>();

        for (Long postId : postIds) {
            String key = VIEW_PREFIX + postId;
            String value = redisTemplate.opsForValue().get(key);

            Long redisCount = (value != null) ? Long.parseLong(value) : 0L;
            result.put(postId, redisCount);
        }

        return result;
    }

    // 게시글 조회수 증가 및 값 반환
    public Long incrementAndGetViewCount(Long postId, Long userId, String viewToken) {

        String uniqueViewerKey;

        // 로그인 사용자
        if (userId != null) {
            uniqueViewerKey = VIEW_PREFIX + postId + ":user:" + userId;
        } 
        // 비로그인 사용자 → 쿠키 기반 UUID
        else {
            uniqueViewerKey = VIEW_PREFIX + postId + ":guest:" + viewToken;
        }


        // 유저가 일정 시간 내 조회했는지 확인
        Boolean hasViewed = redisTemplate.hasKey(uniqueViewerKey);
        if (hasViewed != null && hasViewed) {
            // 이미 조회한 경우, 조회수와 점수는 증가시키지 않음
            return getViewCount(postId);
        }

        String key = VIEW_PREFIX + postId;
        Long viewCount = redisTemplate.opsForValue().increment(key, 1);

        // 유저 조회 기록 저장 (TTL 적용)
        redisTemplate.opsForValue().set(uniqueViewerKey, "1", Duration.ofSeconds(VIEW_TTL_SECONDS));

        return viewCount;
    }

    // 게시글 현재 조회수 조회
    public Long getViewCount(Long postId) {
        String key = VIEW_PREFIX + postId;
        Object value = redisTemplate.opsForValue().get(key);
        return (value != null) ? Long.parseLong(value.toString()) : 0L;
    }

    // ======================
    // 일정 주기마다 MySQL에 반영
    // ======================
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10분마다 실행
    public void flushViewCountsToDB() {
        // Redis에 있는 모든 키 가져오기 (post:view:* 형태)
        Set<String> keys = redisTemplate.keys(VIEW_PREFIX + "*");
        System.out.println("Redis keys: " + keys);
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            String postIdStr = key.replace(VIEW_PREFIX, "");
            // 유저 조회 키는 제외
            if (postIdStr.contains(":user:") || postIdStr.contains(":guest:")) continue;

            Long postId = Long.parseLong(postIdStr);

            String value = redisTemplate.opsForValue().get(key);
            if (value == null) continue;

            Long redisCount = Long.parseLong(value);

            // DB에서 현재 게시글 조회수 가져오기
            postRepository.findById(postId).ifPresent(post -> {
                Long dbCount = post.getViewCount();
                Long updatedCount;

                if (dbCount == 0) {
                    // DB 조회수가 0이면 단순히 Redis 값 추가
                    updatedCount = redisCount;
                } else {
                    // 이미 값이 있으면 기존 DB값 + (Redis 누적값 - DB 반영분)
                    updatedCount = dbCount + (redisCount - dbCount);
                }

                post.setViewCount(updatedCount);
                postRepository.save(post);

            });
        }
    }
}
