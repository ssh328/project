package com.song.project.controller;
import com.song.project.CustomUser;
import com.song.project.service.ReviewService;
import com.song.project.entity.Review;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/review")
    String Review(@RequestParam String content,
                        @RequestParam int rating,
                        @RequestParam Long targetUserId,
                        Authentication auth) {

        CustomUser user = getUserId(auth);
        Review review = reviewService.createReview(content, rating, targetUserId, user);

        String encodedUsername = reviewService.encodeUsername(review.getTargetUser().getUsername());

        return "redirect:/profile/" + encodedUsername + "?tab=reviews";
    }

    @DeleteMapping("/review/{id}")
    ResponseEntity<String> deleteReview(@PathVariable Long id, Authentication auth) {
        CustomUser user = getUserId(auth);
        reviewService.deleteReview(id, user);
        return ResponseEntity.ok("삭제 완료");
    }

    // Authentication에서 사용자 ID 추출
    private CustomUser getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user;
        }
        return null;
    }
}