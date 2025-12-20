package com.song.project.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.security.CustomUser;

import lombok.RequiredArgsConstructor;

/**
 * 좋아요 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final RecommendedPostService recommendedPostService;

    /**
     * 게시글 좋아요 토글 처리
     * 좋아요가 없으면 추가, 있으면 삭제하며 게시글의 좋아요 수를 업데이트
     * @param postId 좋아요를 토글할 게시글 ID
     * @param user 현재 로그인한 사용자 정보
     * @return 좋아요 토글 결과 (loggedIn: true, success: 좋아요 추가 여부, likeCount: 현재 좋아요 수)
     * @throws IllegalArgumentException 게시글을 찾을 수 없는 경우
     */
    public Map<String, Object> toggleLike(Long postId, CustomUser user) {

        // 조회 쿼리를 줄이기 위해 User 엔티티 전체를 조회하지 않고 ID만 가진 참조 객체 사용
        // JPA는 userRef의 id 값만 읽어서 외래키(userId)에 저장
        User userRef = new User();
        userRef.setId(user.id);

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

        // 기존 좋아요 여부 확인
        Optional<Likes> existingLike = likeRepository.findByUserAndPost(userRef, post);
        boolean liked = existingLike.isEmpty();

        if (liked) {
            // 좋아요 추가: 좋아요 수 증가, Likes 엔티티 생성, 추천 점수 증가
            post.setLikeCnt(post.getLikeCnt() + 1);

            Likes newLike = new Likes();
            newLike.setUser(userRef);
            newLike.setPost(post);
            likeRepository.save(newLike);
            recommendedPostService.addLikeScore(postId);
        } else {
            // 좋아요 취소: 좋아요 수 감소, Likes 엔티티 삭제
            post.setLikeCnt(post.getLikeCnt() - 1);
            likeRepository.delete(existingLike.get());
        }

        postRepository.save(post);

        return Map.of("loggedIn", true, "success", liked, "likeCount", post.getLikeCnt());
    } 
}
