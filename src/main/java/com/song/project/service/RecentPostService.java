package com.song.project.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecentPostService {
    // private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_RECENT = 20;

    // public RecentPostService(RedisTemplate<String, Object> redisTemplate) {
    //     this.redisTemplate = redisTemplate;
    // }
    public RecentPostService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 최근 본 상품 추가 (중복 제거)
    public void addRecentPost(Long userId, Long postId) {
        String key = getKey(userId);
        // redisTemplate.opsForList().remove(key, 0, postId);
        redisTemplate.opsForList().remove(key, 0, String.valueOf(postId));
        // redisTemplate.opsForList().leftPush(key, postId);
        redisTemplate.opsForList().leftPush(key, String.valueOf(postId));
        redisTemplate.opsForList().trim(key, 0, MAX_RECENT - 1);
    }

    // 최근 본 상품 목록 조회
    public List<Long> getRecentPosts(Long userId) {
        String key = getKey(userId);
        // List<Object> list = redisTemplate.opsForList().range(key, 0, MAX_RECENT - 1);
        List<String> list = redisTemplate.opsForList().range(key, 0, MAX_RECENT - 1);
        // return list.stream().map(obj -> Long.valueOf(String.valueOf(obj))).collect(Collectors.toList());
        return list.stream().map(Long::valueOf).collect(Collectors.toList());
    }

    private String getKey(Long userId) {
        return "recent:post:" + userId;
    }
}
