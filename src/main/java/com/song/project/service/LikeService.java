package com.song.project.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.song.project.CustomUser;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final RecommendedPostService recommendedPostService;

    public Map<String, Object> toggleLike(Long postId, CustomUser user) {

        // 조회 쿼리를 줄이기 위해 Likes 엔티티는 그대로 유지
        // User 객체 대신 “ID만 가진 더미 객체”를 넘김
        // User 엔티티 전체를 조회하지 않아도 되고,
        // JPA는 userRef의 id 값만 읽어서 외래키(userId)에 저장
        User userRef = new User();
        userRef.setId(user.id);

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        Optional<Likes> existingLike = likeRepository.findByUserAndPost(userRef, post);
        boolean liked = existingLike.isEmpty();

        if (liked) {
            post.setLikeCnt(post.getLikeCnt() + 1);

            Likes newLike = new Likes();
            newLike.setUser(userRef);
            newLike.setPost(post);
            likeRepository.save(newLike);
            recommendedPostService.addLikeScore(postId);
        } else {
            post.setLikeCnt(post.getLikeCnt() - 1);
            likeRepository.delete(existingLike.get());
        }

        postRepository.save(post);

        return Map.of("loggedIn", true, "success", liked, "likeCount", post.getLikeCnt());
    } 
}
