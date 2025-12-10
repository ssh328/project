package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.JwtUtil;
import com.song.project.dto.UserProfileDto;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseCookie;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.song.project.service.UserService;
import com.song.project.service.UserService.ProfileResult;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    // 마이페이지
    @GetMapping("/mypage")
    @PreAuthorize("isAuthenticated()")
    public String myPage() {
        return "user/mypage.html";
    }

    // 내 게시물 목록
    @GetMapping("/my/posts")
    @PreAuthorize("isAuthenticated()")
    public String getMyPosts(Model model, 
                            Authentication auth, 
                            @RequestParam(defaultValue = "1") int page) {

        Long loginUserId = getUserId(auth);

        UserService.MyPostResult result = userService.getMyPosts(loginUserId, page);

        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "user/mypost.html";
    }

    // 내 좋아요 목록
    @GetMapping("/my/likes")
    @PreAuthorize("isAuthenticated()")
    public String getMyLikes(Model model, 
                            Authentication auth, 
                            @RequestParam(defaultValue = "1") int page) {

        Long loginUserId = getUserId(auth);

        UserService.MyLikeResult result = userService.getMyLikes(loginUserId, page);

        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "user/mylike.html";
    }

    // 설정 페이지
    @GetMapping("/setting")
    @PreAuthorize("isAuthenticated()")
    public String setting(Model model, 
                        Authentication auth) {
        Long loginUserId = getUserId(auth);

        UserProfileDto userDto = userService.getUserProfile(loginUserId);
        model.addAttribute("currentUser", userDto);
        return "user/setting.html";
    }

    // 프로필 이미지 업로드
    @PostMapping("/my/{userId}/profile-image")
    @PreAuthorize("isAuthenticated()")
    public String uploadProfileImage(@PathVariable Long userId, 
                                    @RequestParam String image) {
        
        userService.updateUserProfileImage(userId, image);
        return "redirect:/setting";
    }

    // 프로필 페이지
    @GetMapping("/profile/{username}")
    String profileUser(Model model, @PathVariable String username, 
                       @RequestParam(defaultValue = "1") int postPage,
                       @RequestParam(defaultValue = "1") int reviewPage,
                       @RequestParam(defaultValue = "posts") String tab,
                       Authentication auth) {
        
        Long loginUserId = getUserId(auth);

        ProfileResult result = userService.getProfileResult(username, postPage, reviewPage, loginUserId);

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
        
        return "user/profile.html";
    }

    // 이메일/비밀번호 확인 페이지
    @GetMapping("/verify-password")
    @PreAuthorize("isAuthenticated()")
    public String verifyPasswordForm(Model model, 
                                @RequestParam(required = false) String nextAction) {
        model.addAttribute("nextAction", nextAction);
        return "user/verify-password.html";
    }

    // 이메일/비밀번호 확인 -> HttpOnly cookie 발급 후 리다이렉트
    @PostMapping("/verify-password")
    @PreAuthorize("isAuthenticated()")
    public String verifyPassword(@RequestParam String email, 
                                @RequestParam String password, 
                                @RequestParam(required = false) String nextAction, 
                                Authentication auth, 
                                HttpServletResponse response, 
                                RedirectAttributes redirectAttributes) {
        Long loginUserId = getUserId(auth);

        try {
            String verifiedToken = userService.verifyPassword(loginUserId, email, password);

            // HttpOnly cookie 생성
            setVerifiedTokenCookie(response, verifiedToken);

            String redirectUrl = "delete-account".equals(nextAction)
                ? "/delete-account"
                : "/change-password";
            
            redirectAttributes.addFlashAttribute("successMessage", "본인 인증이 완료되었습니다.");
            return "redirect:" + redirectUrl;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            String redirectUrl = "delete-account".equals(nextAction)
                ? "/verify-password?nextAction=delete-account"
                : "/verify-password?nextAction=change-password";
            return "redirect:" + redirectUrl;
        }
    }

    // 비밀번호 변경 페이지
    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePasswordForm() {
        return "user/change-password.html";
    }

    // 비밀번호 변경
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(@CookieValue(value = "verified_token", required = false) String verifiedToken, 
                                 @RequestParam String newPassword, 
                                 Authentication auth, 
                                 RedirectAttributes redirectAttributes, 
                                 HttpServletResponse response) {
        String validationError = validateVerifiedToken(verifiedToken);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/verify-password";
        }

        Long loginUserId = getUserId(auth);

        try {
            userService.changePassword(loginUserId, newPassword);
            
            // 사용 완료된 토큰 쿠키 삭제
            clearVerifiedTokenCookie(response);
            
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/setting";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-password";
        }
    }

    // 계정 삭제 페이지
    @GetMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteAccountForm() {
        return "user/delete-account.html";
    }

    // 계정 삭제
    @PostMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteAccount(@CookieValue(value = "verified_token", required = false) String verifiedToken,
                               Authentication auth,
                               RedirectAttributes redirectAttributes,
                               HttpServletResponse response) {
        String validationError = validateVerifiedToken(verifiedToken);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/verify-password?nextAction=delete-account";
        }

        Long loginUserId = getUserId(auth);

        try {
            userService.deleteAccount(loginUserId);
            
            // 모든 인증 쿠키 삭제
            clearAllAuthCookies(response);
            
            redirectAttributes.addFlashAttribute("successMessage", "계정이 성공적으로 삭제되었습니다.");
            return "redirect:/post/list";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-password?nextAction=delete-account";
        }
    }

    // ===========================
    // 헬퍼 메서드
    // ===========================

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }
        return null;
    }

    // 본인 인증 토큰 쿠키를 설정
    private void setVerifiedTokenCookie(HttpServletResponse response, String verifiedToken) {
        ResponseCookie cookie = ResponseCookie.from("verified_token", verifiedToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(5 * 60) // 5분
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 본인 인증 토큰 쿠키를 삭제
    private void clearVerifiedTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = createResponseCookie("verified_token", "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 모든 인증 관련 쿠키를 삭제 (JWT + verified_token)
    private void clearAllAuthCookies(HttpServletResponse response) {
        ResponseCookie jwtCookie = createResponseCookie("jwt", "", 0);
        ResponseCookie verifiedCookie = createResponseCookie("verified_token", "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, verifiedCookie.toString());
    }

    // ResponseCookie를 생성
    private ResponseCookie createResponseCookie(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    // 본인 인증 토큰을 검증
    // @param verifiedToken 검증할 토큰
    // @return 검증 실패 시 에러 메시지, 성공 시 null
    private String validateVerifiedToken(String verifiedToken) {
        if (verifiedToken == null) {
            return "본인 인증이 필요합니다.";
        }

        // 1차 검증: 토큰이 위조/만료되지 않았는지 확인
        if (!JwtUtil.validateTemporaryToken(verifiedToken)) {
            return "유효하지 않거나 만료된 인증입니다.";
        }

        // 2차 검증: 토큰의 용도(type)가 올바른지 확인
        var claims = JwtUtil.extractToken(verifiedToken);
        if (!"verify".equals(claims.get("type"))) {
            return "잘못된 인증 토큰입니다.";
        }

        return null; // 검증 성공
    }
}