package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.service.RecommendedPostService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LikeController {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final RecommendedPostService recommendedPostService;

    @PostMapping("/like/{post_id}")
    @ResponseBody   // JSON을 직접 반환
    public Map<String, Object> toggleLike(@PathVariable Long post_id,
                                          Authentication auth) {

        Map<String, Object> response = new HashMap<>();

        if (auth == null || !auth.isAuthenticated()) {
            response.put("loggedIn", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        // CustomUser user = (CustomUser) auth.getPrincipal();
        // User user_data = userRepository.findById(user.id)
        //         .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        // 조회 쿼리를 줄이고 싶다면,
        // Likes 엔티티는 그대로 유지하되,
        // User 객체 대신 “ID만 가진 더미 객체”를 넘기면 됩니다
        Long user_data = ((CustomUser) auth.getPrincipal()).id;
        User userRef = new User();
        userRef.setId(user_data);
        // ⬆️ 이렇게 하면
        // User 엔티티 전체를 조회하지 않아도 되고,
        // JPA는 userRef의 id 값만 읽어서 외래키(userId)에 저장합니다.
        
        Post post_data = postRepository.findById(post_id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid post ID"));

        Optional<Likes> existingLike = likeRepository.findByUserAndPost(userRef, post_data);

        boolean liked;
        if (existingLike.isPresent()) {
            post_data.setLikeCnt(post_data.getLikeCnt() - 1);
            postRepository.save(post_data);
            likeRepository.delete(existingLike.get());
            liked = false;
            System.out.println("좋아요 취소");
        } else {
            post_data.setLikeCnt(post_data.getLikeCnt() + 1);
            postRepository.save(post_data);

            Likes newLike = new Likes();
            newLike.setUser(userRef);
            newLike.setPost(post_data);
            likeRepository.save(newLike);

            liked = true;
            System.out.println("좋아요 추가");

            // 🔹 좋아요 추가 시에만 Redis 추천 게시물 가중치 반영
            recommendedPostService.addLikeScore(post_id);
        }

        // JSON으로 내려줄 응답
        response.put("loggedIn", true);
        response.put("success", liked);
        response.put("likeCount", post_data.getLikeCnt());
        return response;
    }
}