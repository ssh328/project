package com.song.project.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.CustomUser;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review createReview(String content, int rating, Long targetUserId, CustomUser customUser) {
        User reviewer = userRepository.findByUsername(customUser.getUsername())
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new NotFoundException("리뷰 대상 사용자를 찾을 수 없습니다."));

        Review review = new Review();
        review.setContent(content);
        review.setTargetUser(targetUser);
        review.setRating(rating);
        review.setReviewer(reviewer);

        try {
            return reviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("리뷰 생성에 실패했습니다.");
        }
    }

    @Transactional
    public void deleteReview(Long reviewId, CustomUser customUser) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다."));

        if (!review.getReviewer().getUsername().equals(customUser.getUsername())) {
            throw new ForbiddenException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
        
        try {
            reviewRepository.delete(review);
        } catch (Exception e) {
            throw new BadRequestException("리뷰 삭제에 실패했습니다.");
        }
    }

    public String encodeUsername(String username) {
        return URLEncoder.encode(
            username,
            StandardCharsets.UTF_8
        ).replaceAll("\\+", "%20");
    }
}
