package com.song.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.song.project.CustomUser;
import com.song.project.service.RecentPostService;
import com.song.project.service.RecentPostService.RecentPostsResult;

import org.springframework.ui.Model;

@Controller
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RequestMapping("/recent-posts")
public class RecentPostController {
    private final RecentPostService recentPostService;

    // 최근 본 상품 ID 추가 - 실제로는 상품 상세 호출할 때 내부에서 호출 추천
    @PostMapping("/add/{postId}")
    public ResponseEntity<?> addRecent(@PathVariable Long postId, Authentication auth) {
        
        System.out.println("요청 들어옴! postId = " + postId);
        
        // 로그인되지 않은 사용자는 무시
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok().build();
        }

        // 실제 서비스에서는 로그인 체크 및 사용자 ID 확인
        Long userId = getUserId(auth);

        System.out.println("로그인 사용자 → userId = " + userId + ", postId = " + postId);

        recentPostService.addRecentPost(userId, postId);

        System.out.println("최근 본 게시글 저장 완료!");

        return ResponseEntity.ok().build();
    }

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

        return "recent-posts.html";
    }

    private Long getUserId(Authentication auth) {
        // CustomUser에서 ID 추출 방식에 맞게 수정 필요
        CustomUser user = (CustomUser) auth.getPrincipal();
        return user.id;
    }
}