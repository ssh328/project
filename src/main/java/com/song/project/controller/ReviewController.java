package com.song.project.controller;
import com.song.project.CustomUser;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository; 

   @PostMapping("/review")
   String Review(@RequestParam String content,
                     @RequestParam int rating,
                     @RequestParam Long targetUserId,
                     Authentication auth) {

       CustomUser user = (CustomUser) auth.getPrincipal();
       User reviewer = userRepository.findByUsername(user.getUsername()).orElseThrow(() -> new IllegalArgumentException("로그인 유저 없음"));

       User targetUser = userRepository.findById(targetUserId)
       .orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

       Review review = new Review();
       review.setContent(content);
       review.setTargetUser(targetUser);
       review.setRating(rating);
       review.setReviewer(reviewer);

       reviewRepository.save(review);

       return "redirect:/profile/" + targetUser.getUsername();
   }

   @DeleteMapping("/review/{id}")
   ResponseEntity<String> deleteReview (@PathVariable Long id, Authentication auth) {
    
    Review review = reviewRepository.findById(id).orElse(null);
    if (review == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                             .body("리뷰를 찾을 수 없습니다.");
    }

    CustomUser user = (CustomUser) auth.getPrincipal();;

    if (!review.getReviewer().getUsername().equals(user.getUsername())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("본인이 작성한 리뷰만 삭제 가능");
    }
    
    reviewRepository.delete(review);
    return ResponseEntity.ok("삭제 완료");
   }
}