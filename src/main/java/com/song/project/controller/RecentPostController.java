package com.song.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.song.project.security.CustomUser;
import com.song.project.service.RecentPostService;
import com.song.project.service.RecentPostService.RecentPostsResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.ui.Model;

/**
 * 최근 본 게시글 관련 API를 제공하는 컨트롤러
 */
@Tag(name = "최근 본 게시글 API", description = "최근 본 게시글 관련 API")
@Controller
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RequestMapping("/recent-posts")
public class RecentPostController {
    private final RecentPostService recentPostService;

    /**
     * 최근 본 게시글 추가
     */
    @Operation(summary = "최근 본 게시글 추가", description = "사용자가 본 게시글을 최근 본 목록에 추가합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/add/{postId}")
    @ResponseBody
    public ResponseEntity<?> addRecent(
        @PathVariable Long postId, 
        Authentication auth) {
        
        // 로그인되지 않은 사용자는 무시
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok().build();
        }

        Long userId = getUserId(auth);

        recentPostService.addRecentPost(userId, postId);

        return ResponseEntity.ok().build();
    }

    /**
     * 최근 본 상품 목록 조회
     */
    @GetMapping
    public String getRecentPosts(Model model, Authentication auth) {

        Long userId = getUserId(auth);

        RecentPostsResult result = recentPostService.getRecentPostsWithDetails(userId);

        model.addAttribute("posts", result.getPosts());
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "user/recent-posts.html";
    }

    // ===========================
    // 헬퍼 메서드
    // ===========================
    
    /**
     * 유저 ID 조회
     */
    private Long getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }   
        return null;
    }
}