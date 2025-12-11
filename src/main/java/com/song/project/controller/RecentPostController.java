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

import org.springframework.ui.Model;

@Controller
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RequestMapping("/recent-posts")
public class RecentPostController {
    private final RecentPostService recentPostService;

    // 최근 본 상품 ID 추가
    @PostMapping("/add/{postId}")
    public ResponseEntity<?> addRecent(@PathVariable Long postId, Authentication auth) {
        
        // 로그인되지 않은 사용자는 무시
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok().build();
        }

        Long userId = getUserId(auth);

        recentPostService.addRecentPost(userId, postId);

        return ResponseEntity.ok().build();
    }

    // 최근 본 상품 목록
    @GetMapping
    public String getRecentPosts(Model model, Authentication auth) {

        // 사용자 ID 추출
        Long userId = getUserId(auth);

        // 서비스에서 모든 비즈니스 로직 처리
        RecentPostsResult result = recentPostService.getRecentPostsWithDetails(userId);

        // 모델에 담기 (뷰 관련 로직만 컨트롤러에 남김)
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "user/recent-posts.html";
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }
        return null;
    }
}