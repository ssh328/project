package com.song.project.controller;

import com.song.project.entity.Post;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.post.PostStatus;
import com.song.project.service.AdminService;
import com.song.project.service.AdminService.DashboardStats;
import com.song.project.service.PostService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final AdminService adminService;
    private final PostService postService;

    // 대시보드
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStats stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    // 사용자 목록
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

    // 게시글 목록
    @GetMapping("/posts")
    public String posts(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String status,
                        Model model) {

        PostStatus postStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                postStatus = PostStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
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
        model.addAttribute("categories", postService.getCategories());

        return "admin/posts";
    }

    // 리뷰 목록
    @GetMapping("/reviews")
    public String reviews(@RequestParam(defaultValue = "1") int page,
                          Model model) {
        Page<Review> reviews = adminService.getReviews(page, 20);
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reviews.getTotalPages());
        return "admin/reviews";
    }

    // 게시글 삭제
    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "1") int page,
                             @RequestParam(required = false) String keyword) {
        adminService.deletePostAsAdmin(id);
        return "redirect:/admin/posts?page=" + page +
                (keyword != null ? "&keyword=" + keyword : "");
    }

    // 리뷰 삭제
    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam(defaultValue = "1") int page) {
        adminService.deleteReviewAsAdmin(id);
        return "redirect:/admin/reviews?page=" + page;
    }
}