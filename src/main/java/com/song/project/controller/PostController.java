package com.song.project.controller;

import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostEditDto;
import com.song.project.dto.PostListDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostStatus;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.security.CustomUser;
import com.song.project.service.PostService;
import com.song.project.service.PostService.PostDetailResult;
import com.song.project.service.PostService.PostListResult;
import com.song.project.service.PostService.PostStatusParseResult;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 게시글 관련 API를 제공하는 컨트롤러
 */
@Tag(name = "게시글 API", description = "게시글 관련 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;

    /**
     * 게시물 리스트 조회
     */
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

        return "post/list.html";
    }

    /**
     * 게시물 검색 조회
     */
    @GetMapping("/search")
    String search(Model model, Authentication auth,
            @RequestParam String searchText,
            @RequestParam(defaultValue = "1") int page) {
        
        Page<PostListDto> postDtos = postService.searchPosts(searchText, page);

        Long userId = getUserId(auth);

        // 검색 결과
        SearchResult result = postService.getSearchResult(postDtos, userId);

        model.addAttribute("posts", postDtos);
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postDtos.getTotalPages());
        model.addAttribute("searchText", searchText);

        return "post/search.html";
    }

    /**
     * 게시물 상세 조회
     */
    @GetMapping("/detail/{id}")
    String show_post(Model model, @PathVariable Long id,  
                        Authentication auth,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        Long loginUserId = getUserId(auth);

        // 비로그인 사용자용 viewToken 처리
        String viewToken = null;
        if (loginUserId == null) {
            viewToken = getOrCreateViewToken(request, response);
        }

        // 상세 페이지 결과
        PostDetailResult result = postService.getPostDetailResult(id, loginUserId, viewToken);

        model.addAttribute("data", result.getPost());
        model.addAttribute("postWriterId", result.getPostWriterId());
        model.addAttribute("loginUserId", loginUserId);
        model.addAttribute("viewCount", result.getViewCount());
        model.addAttribute("recommendedPosts", result.getRecommendedPosts());

        return "post/detail.html";
    }

    /**
     * 게시물 추가 페이지 조회
     */
    @GetMapping("/new-post")
    @PreAuthorize("isAuthenticated()")
    String add_post(Model model) {
        model.addAttribute("categories", postService.getCategories());
        return "post/add.html";
    }

    /**
     * 게시물 추가 처리
     */
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    String addPost(@Valid @ModelAttribute PostCreateDto dto,
                   BindingResult bindingResult,
                   Authentication auth,
                   RedirectAttributes redirectAttributes) {
        
        // 유효성 검사 실패 시 처리
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/post/new-post";
        }
        
        // 게시물 추가 처리
        try {
            Long userId = getUserId(auth);
            postService.createPost(userId, dto);
            redirectAttributes.addFlashAttribute("successMessage", "게시물이 등록되었습니다.");
            return "redirect:/post/list";
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/new-post";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/new-post";
        }
    }

    /**
     * 게시물 수정 페이지 조회
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    String edit(Model model, @PathVariable Long id, Authentication auth) {

        Long userId = getUserId(auth);
        Post post = postService.getPostForEdit(id, userId);

        PostEditDto postEditDto = PostEditDto.from(post);
        model.addAttribute("data", postEditDto);
        model.addAttribute("categories", postService.getCategories());
        return "post/edit.html";
    }

    /**
     * 게시물 수정 처리
     */
    @PostMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    String editPost(@Valid @ModelAttribute PostUpdateDto dto,
                    BindingResult bindingResult,
                    Authentication auth,
                    RedirectAttributes redirectAttributes) {
        
        // 유효성 검사 실패 시 처리
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/post/edit/" + dto.getPostId();
        }
        
        // 게시물 수정 처리
        try {
            Long userId = getUserId(auth);
            postService.updatePost(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage", "게시물이 수정되었습니다.");
            return "redirect:/post/detail/" + dto.getPostId();
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/list";
        } catch (UnauthorizedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/post/edit/" + dto.getPostId();
        }
    }

    /**
     * 게시물 삭제 처리
     */
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    ResponseEntity<String> delete(@RequestParam Long id, Authentication auth) {

        // 게시물 삭제 처리
        try {
            Long userId = getUserId(auth);
            postService.deletePost(id, userId);
            return ResponseEntity.ok("삭제완료");
        } catch (NotFoundException | ForbiddenException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * 게시물 상태 변경 처리
     * 게시글의 판매 상태를 변경 (판매중/예약중/판매완료)
     */
    @Operation(summary = "게시글 상태 변경", description = "게시글의 판매 상태를 변경합니다 (판매중/예약중/판매완료)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상태 변경 성공",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"success\": true, \"message\": \"상태가 변경되었습니다.\", \"status\": \"SOLD\", \"statusDescription\": \"판매완료\"}"))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    ResponseEntity<Map<String, Object>> updateStatus(
        @PathVariable Long id,
        @Valid @ModelAttribute PostStatusUpdateDto dto,
        BindingResult bindingResult,
        Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        
        // 유효성 검사 실패 시 처리
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

    /**
     * 쿠키에서 viewToken 가져오기 또는 생성
     */
    private String getOrCreateViewToken(HttpServletRequest request, HttpServletResponse response) {
        
        // 쿠키에서 viewToken 가져오기
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