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
 * 게시글 조회, 생성, 수정, 삭제, 상태 변경 등의 기능을 제공
 */
@Tag(name = "게시글 API", description = "게시글 관련 API")
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;

    /**
     * 게시물 목록을 페이지네이션과 함께 조회
     * 카테고리, 가격 범위, 상태, 정렬 기준으로 필터링 가능
     * @param page 페이지 번호 (기본값: 1)
     * @param category 카테고리 필터 (선택적)
     * @param sort_by 정렬 기준 (기본값: 최신순, "hottest": 인기순, 선택적)
     * @param start_price 최소 가격 필터 (선택적)
     * @param end_price 최대 가격 필터 (선택적)
     * @param status 게시글 상태 필터 (ON_SALE, RESERVED, SOLD, 선택적)
     * @return 게시물 목록 템플릿 경로
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
        
        // 상태 문자열을 PostStatus enum으로 변환
        PostStatusParseResult statusResult = postService.getPostStatusParseResult(status);

        // 필터 조건에 맞는 게시물 목록 조회
        Page<PostListDto> postDtos = postService.getPosts(category, start_price, end_price,
                    statusResult.getPostStatus(), page, sort_by);

        Long userId = getUserId(auth);
                    
        // 좋아요 여부, 조회수 등 추가 정보 포함한 결과 생성
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
     * 게시물을 검색어로 조회
     * MySQL Full-Text Search를 사용하여 제목에서 검색
     * @param searchText 검색어 (필수)
     * @param page 페이지 번호 (기본값: 1)
     * @return 검색 결과 템플릿 경로
     */
    @GetMapping("/search")
    String search(Model model, Authentication auth,
            @RequestParam String searchText,
            @RequestParam(defaultValue = "1") int page) {
        
                
        // 검색어와 페이지 번호를 전달 -> 해당 키워드가 포함된 게시물 페이지를 조회
        Page<PostListDto> postDtos = postService.searchPosts(searchText, page);

        Long userId = getUserId(auth);

        // 좋아요 여부 등 추가 정보 포함한 검색 결과 생성
        SearchResult result = postService.getSearchResult(postDtos, userId);

        model.addAttribute("posts", postDtos);
        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postDtos.getTotalPages());
        model.addAttribute("searchText", searchText);

        return "post/search.html";
    }

    /**
     * 게시물 상세 정보를 조회
     * 조회수 증가, 추천 게시글 목록 포함
     * 비로그인 사용자는 쿠키 기반 viewToken으로 조회수 추적
     * @param id 게시물 ID
     * @return 게시물 상세 템플릿 경로
     */
    @GetMapping("/detail/{id}")
    String show_post(Model model, @PathVariable Long id,  
                        Authentication auth,
                        HttpServletRequest request,
                        HttpServletResponse response) {

        Long loginUserId = getUserId(auth);

        // 비로그인 사용자는 쿠키의 viewToken으로 조회수 추적 (중복 조회 방지)
        String viewToken = null;
        if (loginUserId == null) {
            viewToken = getOrCreateViewToken(request, response);
        }

        // 게시물 상세 정보, 조회수, 추천 게시글 등 포함한 결과 조회
        PostDetailResult result = postService.getPostDetailResult(id, loginUserId, viewToken);

        model.addAttribute("data", result.getPost());
        model.addAttribute("postWriterId", result.getPostWriterId());
        model.addAttribute("loginUserId", loginUserId);
        model.addAttribute("viewCount", result.getViewCount());
        model.addAttribute("recommendedPosts", result.getRecommendedPosts());

        return "post/detail.html";
    }

    /**
     * 게시물 작성 페이지를 조회
     * 인증된 사용자만 접근 가능
     * @return 게시물 작성 템플릿 경로
     */
    @GetMapping("/new-post")
    @PreAuthorize("isAuthenticated()")
    String add_post(Model model) {
        model.addAttribute("categories", postService.getCategories());
        return "post/add.html";
    }

    /**
     * 게시물을 생성
     * 인증된 사용자만 접근 가능, 유효성 검사 후 게시물 생성
     * @param dto 게시물 생성 정보
     * @param bindingResult 유효성 검사 결과
     * @return 게시물 목록 페이지로 리다이렉트 (성공 시) 또는 작성 페이지로 리다이렉트 (실패 시)
     */
    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    String addPost(@Valid @ModelAttribute PostCreateDto dto,
                   BindingResult bindingResult,
                   Authentication auth,
                   RedirectAttributes redirectAttributes) {
        
        // 유효성 검사 실패 시 첫 번째 에러 메시지를 flash attribute로 전달
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/post/new-post";
        }
        
        // 게시물 생성 처리
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
     * 게시물 수정 페이지를 조회
     * 작성자만 접근 가능, 인증된 사용자만 접근 가능
     * @param id 수정할 게시물 ID
     * @return 게시물 수정 템플릿 경로
     */
    @GetMapping("/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    String edit(Model model, @PathVariable Long id, Authentication auth) {

        Long userId = getUserId(auth);

        // 게시물 수정 폼에 사용할 게시글 정보를 작성자 본인만 조회
        Post post = postService.getPostForEdit(id, userId);

        PostEditDto postEditDto = PostEditDto.from(post);
        model.addAttribute("data", postEditDto);
        model.addAttribute("categories", postService.getCategories());
        return "post/edit.html";
    }

    /**
     * 게시물을 수정
     * 작성자만 수정 가능, 인증된 사용자만 접근 가능, 유효성 검사 후 게시물 수정
     * @param dto 게시물 수정 정보
     * @param bindingResult 유효성 검사 결과
     * @return 게시물 상세 페이지로 리다이렉트 (성공 시) 또는 수정 페이지/목록 페이지로 리다이렉트 (실패 시)
     */
    @PostMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    String editPost(@Valid @ModelAttribute PostUpdateDto dto,
                    BindingResult bindingResult,
                    Authentication auth,
                    RedirectAttributes redirectAttributes) {
        
        // 유효성 검사 실패 시 첫 번째 에러 메시지를 flash attribute로 전달
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return "redirect:/post/edit/" + dto.getPostId();
        }
        
        // 게시물 수정 처리
        try {
            Long userId = getUserId(auth);
            
            // 사용자가 작성한 정보와 인증된 사용자 ID를 전달 -> 게시물 수정 처리
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
     * 게시물을 삭제
     * 작성자만 삭제 가능, 인증된 사용자만 접근 가능
     * @param id 삭제할 게시물 ID
     * @return 삭제 성공 메시지 (200) 또는 에러 메시지 (403)
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

        // 작성자 권한 확인 후 게시물 삭제
        try {
            Long userId = getUserId(auth);
            postService.deletePost(id, userId);
            return ResponseEntity.ok("삭제완료");
        } catch (NotFoundException | ForbiddenException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * 게시물의 판매 상태를 변경
     * 작성자만 변경 가능, 인증된 사용자만 접근 가능
     * 상태: ON_SALE(판매중), RESERVED(예약중), SOLD(판매완료)
     * @param id 상태를 변경할 게시물 ID
     * @param dto 상태 변경 정보
     * @param bindingResult 유효성 검사 결과
     * @return 상태 변경 결과 JSON (성공 시 200, 실패 시 400/403)
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
        
        // 유효성 검사 실패 시 첫 번째 에러 메시지를 응답에 포함
        if (bindingResult.hasErrors()) {
            response.put("success", false);
            response.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.status(400).body(response);
        }
        
        Long userId = getUserId(auth);

        // 게시물 ID, 상태 변경 정보, 인증된 사용자 ID를 전달 -> 상태 변경 처리
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
     * 인증 정보에서 사용자 ID를 추출
     * 인증되지 않은 사용자는 null 반환
     * @param auth 인증 정보
     * @return 사용자 ID (인증된 경우), null (인증되지 않은 경우)
     */
    private Long getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }
        return null;
    }

    /**
     * 쿠키에서 viewToken을 조회하거나 없으면 새로 생성
     * 비로그인 사용자의 조회수 중복 카운트 방지를 위한 토큰
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 기존 viewToken 또는 새로 생성한 viewToken
     */
    private String getOrCreateViewToken(HttpServletRequest request, HttpServletResponse response) {
        
        // 쿠키에서 기존 viewToken 조회
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("viewToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // viewToken이 없으면 UUID로 새로 생성하고 쿠키에 저장 (1일 유효)
        String viewToken = UUID.randomUUID().toString();
        Cookie cookie = new Cookie("viewToken", viewToken);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // 1일 유효
        response.addCookie(cookie);
        return viewToken;
    }
}