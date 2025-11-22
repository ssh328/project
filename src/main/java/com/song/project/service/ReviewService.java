package com.song.project.service;

import org.springframework.stereotype.Service;

import com.song.project.CustomUser;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public Review createReview(String content, int rating, Long targetUserId, CustomUser customUser) {
        User reviewer = userRepository.findByUsername(customUser.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("로그인 유저 없음"));

        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("리뷰 대상 사용자를  찾을 수 없습니다."));

        Review review = new Review();
        review.setContent(content);
        review.setTargetUser(targetUser);
        review.setRating(rating);
        review.setReviewer(reviewer);

        return reviewRepository.save(review);
    }

}
