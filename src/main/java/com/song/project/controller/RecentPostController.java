package com.song.project.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.song.project.CustomUser;
import com.song.project.dto.PostListDto;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.service.PostViewCountService;
import com.song.project.service.RecentPostService;

import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@RequestMapping("/recent-posts")
public class RecentPostController {
    private final RecentPostService recentPostService;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;

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

        // Redis에서 최근 본 상품 ID 목록 가져오기
        List<Long> ids = recentPostService.getRecentPosts(userId);

        // DB에서 해당 게시글(최근에 본 순서)대로 정렬
        Map<Long, PostListDto> postMap = postRepository.findAllById(ids).stream()
                .map(PostListDto::from)
                .collect(Collectors.toMap(PostListDto::getId, dto -> dto));

        List<PostListDto> posts = ids.stream()
                .map(postMap::get)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        // 조회수 합산용 처리
        List<Long> postIds = posts.stream()
        .map(PostListDto::getId)
        .collect(Collectors.toList());

        // Redis 조회수 가져오기
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        // 좋아요한 게시글 ID 목록
        List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());

        // 모델에 담기
        model.addAttribute("posts", posts);
        model.addAttribute("likedPostIds", likedPostIds);
        model.addAttribute("viewCounts", redisViewCounts);

        return "recent-posts.html"; // 최근 본 게시글 템플릿
    }

    private Long getUserId(Authentication auth) {
        // CustomUser에서 ID 추출 방식에 맞게 수정 필요
        CustomUser user = (CustomUser) auth.getPrincipal();
        return user.id;
    }
}