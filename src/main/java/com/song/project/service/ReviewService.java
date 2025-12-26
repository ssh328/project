package com.song.project.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;
import com.song.project.security.CustomUser;

import lombok.RequiredArgsConstructor;

/**
 * 리뷰 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 생성
     * @param content 리뷰 내용
     * @param rating 평점
     * @param targetUserId 리뷰 대상 사용자 ID
     * @param customUser 리뷰 작성자 정보
     * @return 생성된 리뷰
     * @throws NotFoundException 사용자 또는 리뷰 대상을 찾을 수 없는 경우
     * @throws BadRequestException 리뷰 생성 실패 시
     */
    @Transactional
    public Review createReview(String content, int rating, Long targetUserId, CustomUser customUser) {

        User reviewer = userRepository.findByUsername(customUser.getUsername())
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 리뷰 대상 사용자 조회
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

    /**
     * 리뷰 삭제 공통 로직
     * DB 삭제를 수행하는 공통 메서드
     * 일반 사용자용 deleteReview()와 관리자용 AdminService.deleteReviewAsAdmin()에서 공통으로 사용
     * @param review 삭제할 Review 엔티티 (이미 조회된 상태여야 함)
     * @throws BadRequestException 리뷰 삭제 실패 시
     */
    @Transactional
    public void deleteReviewInternal(Review review) {
        try {
            reviewRepository.delete(review);
        } catch (Exception e) {
            throw new BadRequestException("리뷰 삭제에 실패했습니다.");
        }
    }

    /**
     * 리뷰 삭제
     * 작성자만 삭제 가능
     * @param reviewId 삭제할 리뷰 ID
     * @param customUser 현재 로그인한 사용자 정보
     * @throws NotFoundException 리뷰를 찾을 수 없는 경우
     * @throws ForbiddenException 작성자가 아닌 경우
     * @throws BadRequestException 리뷰 삭제 실패 시
     */
    @Transactional
    public void deleteReview(Long reviewId, CustomUser customUser) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new NotFoundException("리뷰를 찾을 수 없습니다."));

        // 작성자 권한 확인
        if (!review.getReviewer().getUsername().equals(customUser.getUsername())) {
            throw new ForbiddenException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
        
        deleteReviewInternal(review);
    }

    /**
     * 사용자명을 URL 인코딩
     * URL 파라미터로 사용하기 위해 인코딩하며, 공백은 %20으로 변환
     * @param username 인코딩할 사용자명
     * @return URL 인코딩된 사용자명
     */
    public String encodeUsername(String username) {
        return URLEncoder.encode(
            username,
            StandardCharsets.UTF_8
        ).replaceAll("\\+", "%20");
    }
}
