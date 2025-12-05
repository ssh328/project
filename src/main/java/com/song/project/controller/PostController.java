package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostEditDto;
import com.song.project.dto.PostListDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.entity.Post;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.post.PostStatus;
import com.song.project.service.PostService;
import com.song.project.service.PostService.PostDetailResult;
import com.song.project.service.PostService.PostListResult;
import com.song.project.service.PostService.PostStatusParseResult;
import com.song.project.service.PostService.ProfileResult;
import com.song.project.service.PostService.SearchResult;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final S3Service s3Service;

    @GetMapping("/list")
    String all_post(Model model,
                    Authentication auth,
                    @RequestParam(defaultValue = "1") int page,
                    @RequestParam(required = false) String category,
                    @RequestParam(required = false) String sort_by,
                    @RequestParam(required = false) Integer start_price,
                    @RequestParam(required = false) Integer end_price,
                    @RequestParam(required = false) String status) {
        
        // 상태 파라미터 파싱
        PostStatusParseResult statusResult = postService.getPostStatusParseResult(status);

        // 게시물 목록 조회
        Page<PostListDto> postDtos = postService.getPosts(category, start_price, end_price,
                    statusResult.getPostStatus(), page, sort_by);

        Long userId = getUserId(auth);
                    
        // 게시물 목록 결과
        PostListResult result = postService.getPostListResult(postDtos, userId);

        // 모델 설정
        model.addAttribute("posts", postDtos);
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("viewCounts", result.getViewCounts());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postDtos.getTotalPages());
        model.addAttribute("categories", postService.getCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedSort", sort_by);
        model.addAttribute("selectedStartPrice", start_price);
        model.addAttribute("selectedEndPrice", end_price);
        model.addAttribute("selectedStatus", statusResult.getSelectedStatus());
        model.addAttribute("statuses", PostStatus.values());

        return "list.html";
    }

    @GetMapping("/search")
    String search(Model model, Authentication auth,
            @RequestParam String searchText,
            @RequestParam(defaultValue = "1") int page) {
        
        // 게시글 검색
        Page<PostListDto> postDtos = postService.searchPosts(searchText, page);

        Long userId = getUserId(auth);

        // 검색 결과
        SearchResult result = postService.getSearchResult(postDtos, userId);

        // 모델 설정
        model.addAttribute("posts", postDtos);
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postDtos.getTotalPages());
        model.addAttribute("searchText", searchText);

        return "search.html";
    }

    @GetMapping("/detail/{id}")
    String show_post(Model model, @PathVariable Long id,  
                        Authentication auth,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        // 로그인 사용자 ID
        Long loginUserId = getUserId(auth);

        // 비로그인 사용자용 viewToken 처리
        String viewToken = null;
        if (loginUserId == null) {
            viewToken = getOrCreateViewToken(request, response);
        }

        // 상세 페이지 결과
        PostDetailResult result = postService.getPostDetailResult(id, loginUserId, viewToken);

        // 모델 설정
        model.addAttribute("data", result.getPost());
        model.addAttribute("postWriterId", result.getPostWriterId());
        model.addAttribute("loginUserId", loginUserId);
        model.addAttribute("viewCount", result.getViewCount());
        model.addAttribute("recommendedPosts", result.getRecommendedPosts());

        return "detail.html";
    }

    @GetMapping("/new-post")
    @PreAuthorize("isAuthenticated()")
    String add_post(Model model) {
        model.addAttribute("categories", postService.getCategories());
        return "add.html";
    }

    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    String addPost(@Valid @ModelAttribute PostCreateDto dto,
                   BindingResult bindingResult,
                   Authentication auth,
                   RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/new-post";
        }
        
        try {
            Long userId = getUserId(auth);
            postService.createPost(userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "게시물이 등록되었습니다.");
            return "redirect:/list";
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/new-post";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/new-post";
        }
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    String getURL(@RequestParam String filename) {

        // 확장자 추출
        String extension = "";

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex != -1) {
            extension = filename.substring(dotIndex);
        }

        // UUID로 unique 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // S3에 저장될 경로 구성
        String key = "project/" + uniqueFileName;

        // Presigned URL 생성
        String result = s3Service.createPresignedUrl(key);

        return result;
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    String edit(Model model, @PathVariable Long id, Authentication auth) {

        Long userId = getUserId(auth);
        Post post = postService.getPostForEdit(id, userId);

        PostEditDto postEditDto = PostEditDto.from(post);
        model.addAttribute("data", postEditDto);
        model.addAttribute("categories", postService.getCategories());
        return "edit.html";
    }

    @PostMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    String editPost(@Valid @ModelAttribute PostUpdateDto dto,
                    BindingResult bindingResult,
                    Authentication auth,
                    RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/edit/" + dto.getPostId();
        }
        
        try {
            Long userId = getUserId(auth);
            postService.updatePost(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage", "게시물이 수정되었습니다.");
            return "redirect:/detail/" + dto.getPostId();
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/list";
        } catch (UnauthorizedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/edit/" + dto.getPostId();
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<String> delete(@RequestParam Long id, Authentication auth) {

        Long userId = getUserId(auth);
        postService.deletePost(id, userId);
        return ResponseEntity.ok("삭제완료");
    }

    @DeleteMapping("/delete-image")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<String> deleteImages(@RequestParam Long imageId, Authentication auth) {

        Long userId = getUserId(auth);
        postService.deleteImage(imageId, userId);
        return ResponseEntity.ok("삭제완료");
    }

    @PatchMapping("/post/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                     @Valid @ModelAttribute PostStatusUpdateDto dto,
                                                     BindingResult bindingResult,
                                                     Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        if (bindingResult.hasErrors()) {
            response.put("success", false);
            response.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(400).body(response);
        }
        
        Long userId = getUserId(auth);
        PostStatus postStatus = postService.updateStatus(id, dto, userId);
        
        response.put("success", true);
        response.put("message", "상태가 변경되었습니다.");
        response.put("status", postStatus.name());
        response.put("statusDescription", postStatus.getDescription());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile/{username}")
    String profileUser(Model model, @PathVariable String username, 
                       @RequestParam(defaultValue = "1") int postPage,
                       @RequestParam(defaultValue = "1") int reviewPage,
                       @RequestParam(defaultValue = "posts") String tab,
                       Authentication auth) {
        
        Long loginUserId = getUserId(auth);

        ProfileResult result = postService.getProfileResult(username, postPage, reviewPage, loginUserId);

        // 모델 설정
        model.addAttribute("user", result.getUser());
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("reviews", result.getReviews());
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("postCurrentPage", postPage);
        model.addAttribute("reviewCurrentPage", reviewPage);
        model.addAttribute("postTotalPages", result.getPostTotalPages());
        model.addAttribute("reviewTotalPages", result.getReviewTotalPages());
        model.addAttribute("loginUserId", result.getLoginUserId());
        model.addAttribute("viewCounts", result.getViewCounts());
        model.addAttribute("tab", tab);
        
        return "profile.html";
    }

    // 헬퍼 메서드

    // Authentication에서 사용자 ID 추출
    private Long getUserId(Authentication auth) {

        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }
        return null;
    }

    // 쿠키에서 viewToken 가져오기 또는 생성
    private String getOrCreateViewToken(HttpServletRequest request, HttpServletResponse response) {
        
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("viewToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // viewToken이 없으면 생성
        String viewToken = UUID.randomUUID().toString();
        Cookie cookie = new Cookie("viewToken", viewToken);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // 1일 유효
        response.addCookie(cookie);
        return viewToken;
    }
}