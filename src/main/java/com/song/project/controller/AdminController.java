package com.song.project.controller;

import com.song.project.entity.Post;
import com.song.project.entity.PostStatus;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.NotFoundException;
import com.song.project.service.AdminService;
import com.song.project.service.AdminService.DashboardStats;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.song.project.service.post.PostQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 관리자 관련 API를 제공하는 컨트롤러
 */
@Tag(name = "관리자 API", description = "관리자 관련 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final PostQueryService postQueryService;

    /**
     * 관리자 대시보드 페이지를 조회
     * 전체 통계 정보(사용자 수, 게시글 수, 리뷰 수 등)를 조회하여 표시
     * @return 관리자 대시보드 템플릿 경로
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStats stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    /**
     * 사용자 목록을 페이지네이션과 함께 조회
     * 키워드가 제공된 경우 사용자명 또는 이메일로 검색
     * @param page 페이지 번호 (기본값: 1)
     * @param keyword 검색 키워드 (사용자명 또는 이메일, 선택적)
     * @return 사용자 목록 템플릿 경로
     */
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(required = false) String keyword,
                        Model model) {
        Page<User> users = adminService.getUsers(keyword, page, 20);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        return "admin/users";
    }

    /**
     * 게시물 목록을 페이지네이션과 함께 조회
     * 키워드, 카테고리, 상태로 필터링
     * @param page 페이지 번호 (기본값: 1)
     * @param keyword 검색 키워드 (게시글 제목, 선택적)
     * @param category 카테고리 필터 (선택적)
     * @param status 게시글 상태 필터 (ON_SALE, RESERVED, SOLD, 선택적)
     * @return 게시물 목록 템플릿 경로
     */
    @GetMapping("/posts")
    public String posts(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String status,
                        Model model) {
        PostStatus postStatus = null;

        // 상태 문자열을 PostStatus enum으로 변환
        // 잘못된 값이 입력된 경우 무시하고 null로 처리 (전체 조회)
        if (status != null && !status.isBlank()) {
            try {
                postStatus = PostStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 잘못된 상태 값은 무시하고 전체 조회
            }
        }

        Page<Post> posts = adminService.getPosts(keyword, category, postStatus, page, 20);

        model.addAttribute("posts", posts);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("categories", postQueryService.getCategories());

        return "admin/posts";
    }

    /**
     * 리뷰 목록을 페이지네이션과 함께 조회
     * @param page 페이지 번호 (기본값: 1)
     * @return 리뷰 목록 템플릿 경로
     */
    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "1") int page,
                          Model model) {
        Page<Review> reviews = adminService.getReviews(page, 20);
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviews.getTotalPages());
        return "admin/reviews";
    }

    /**
     * 관리자 권한으로 게시물을 삭제
     * 삭제 후 게시물 목록 페이지로 리다이렉트, 이전 검색 조건을 유지
     * @param id 삭제할 게시물의 ID
     * @param page 삭제 후 이동할 페이지 번호 (기본값: 1)
     * @param keyword 삭제 후 유지할 검색 키워드 (선택적)
     * @return 게시물 목록 페이지로의 리다이렉트 URL
     */
    @Operation(summary = "관리자 권한으로 게시물을 삭제", description = "관리자 권한으로 게시물을 soft delete 합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "게시물 없음")
    })
    @DeleteMapping("/posts/{id}/delete")
    @ResponseBody
    public ResponseEntity<String> deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String keyword) {
        try {
            adminService.deletePostAsAdmin(id);
            return ResponseEntity.ok("삭제완료");
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/restore")
    @ResponseBody
    public ResponseEntity<String> restorePost(@PathVariable Long id) {
        try {
            adminService.restorePostAsAdmin(id);
            return ResponseEntity.ok("복구완료");
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/posts/{id}/hard-delete")
    @ResponseBody
    public ResponseEntity<String> hardDeletePost(@PathVariable Long id) {
        try {
            adminService.hardDeletePostAsAdmin(id);
            return ResponseEntity.ok("영구삭제완료");
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 관리자 권한으로 리뷰를 삭제
     * 삭제 후 리뷰 목록 페이지로 리다이렉트, 이전 페이지 번호를 유지
     * @param id 삭제할 리뷰의 ID
     * @param page 삭제 후 이동할 페이지 번호 (기본값: 1)
     * @return 리뷰 목록 페이지로의 리다이렉트 URL
     */
    @Operation(summary = "관리자 권한으로 리뷰를 삭제", description = "관리자 권한으로 리뷰를 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "리뷰 없음")
    })
    @DeleteMapping("/reviews/{id}/delete")
    @ResponseBody
    public ResponseEntity<String> deleteReview(@PathVariable Long id,
                                                @RequestParam(defaultValue = "1") int page) {
        try {
            adminService.deleteReviewAsAdmin(id);
            return ResponseEntity.ok("삭제완료");
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}