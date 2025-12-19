package com.song.project.controller;
import com.song.project.service.ReviewService;
import com.song.project.entity.Review;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.security.CustomUser;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 리뷰 관련 API를 제공하는 컨트롤러
 */
@Tag(name = "리뷰 API", description = "리뷰 관련 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {
    private final ReviewService reviewService;

    /**
     * 리뷰 생성 처리
     */
    @PostMapping
    String Review(@RequestParam String content,
                        @RequestParam int rating,
                        @RequestParam Long targetUserId,
                        Authentication auth,
                        RedirectAttributes redirectAttributes) {

        // 리뷰 생성 처리
        try {
            CustomUser user = getUserId(auth);
            Review review = reviewService.createReview(content, rating, targetUserId, user);

            String encodedUsername = reviewService.encodeUsername(review.getTargetUser().getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "리뷰가 등록되었습니다.");

            return "redirect:/post/profile/" + encodedUsername + "?tab=reviews";
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/list";
        }
    }

    /**
     * 리뷰 삭제 처리
     */
    @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{id}")
    @ResponseBody
    ResponseEntity<String> deleteReview(
        @PathVariable Long id, 
        Authentication auth) {

        // 리뷰 삭제 처리
        try {
            CustomUser user = getUserId(auth);
            reviewService.deleteReview(id, user);
            return ResponseEntity.ok("삭제 완료");
        } catch (NotFoundException | ForbiddenException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // ===========================
    // 헬퍼 메서드
    // ===========================
    
    /**
     * 유저 ID 조회
     */
    private CustomUser getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user;
        }
        return null;
    }
}