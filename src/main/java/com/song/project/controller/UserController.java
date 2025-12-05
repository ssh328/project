package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.JwtUtil;
import com.song.project.dto.UserProfileDto;
import com.song.project.exception.BadRequestException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseCookie;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

import com.song.project.service.UserService;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @GetMapping("/register")
    public String register() {
        return "register.html";
    }

    @PostMapping("/user-register")
    public String addUser(@RequestParam String user_id,
                        @RequestParam String password,
                        @RequestParam String username,
                        @RequestParam String email,
                        RedirectAttributes redirectAttributes,
                        HttpServletResponse response) {
        
        try {
            // 회원가입 처리
            userService.register(user_id, password, username, email);
            
            // 자동 로그인 처리
            var authToken = new UsernamePasswordAuthenticationToken(username, password);
            var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            setJwtCookie(response);
    
            redirectAttributes.addFlashAttribute("successMessage", "환영합니다!");
            return "redirect:/list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }

    @PostMapping("/login/jwt")
    @ResponseBody
    public Map<String, Object> loginJWT(@RequestBody Map<String, String> data,
                                        HttpServletResponse response) {
        
        // 인증 처리
        var authToken = new UsernamePasswordAuthenticationToken(
            data.get("username"), data.get("password"));
        var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // JWT 쿠키 설정
        setJwtCookie(response);

        return Map.of("success", true);
    }

    @PostMapping("/logout/jwt")
    public String logoutJWT(HttpServletResponse response) {
        clearJwtCookie(response);
        return "redirect:/list";
    }

    @GetMapping("/my-page")
    @PreAuthorize("isAuthenticated()")
    public String myPage() {
        return "mypage.html";
    }

    @GetMapping("/my/posts")
    @PreAuthorize("isAuthenticated()")
    public String getMyPosts(Model model, 
                            Authentication auth, 
                            @RequestParam(defaultValue = "1") int page) {

        CustomUser user = (CustomUser) auth.getPrincipal();

        UserService.MyPostResult result = userService.getMyPosts(user.id, page);

        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "mypost.html";
    }

    @GetMapping("/my/likes")
    @PreAuthorize("isAuthenticated()")
    public String getMyLikes(Model model, 
                            Authentication auth, 
                            @RequestParam(defaultValue = "1") int page) {

        CustomUser user = (CustomUser) auth.getPrincipal();

        UserService.MyLikeResult result = userService.getMyLikes(user.id, page);

        model.addAttribute("likedPostIds", result.getLikedPostIds());
        model.addAttribute("posts", result.getPosts());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("viewCounts", result.getViewCounts());

        return "mylike.html";
    }

    @GetMapping("/setting")
    @PreAuthorize("isAuthenticated()")
    public String setting(Model model, 
                        Authentication auth) {
        CustomUser user = (CustomUser) auth.getPrincipal();

        UserProfileDto userDto = userService.getUserProfile(user.id);
        model.addAttribute("currentUser", userDto);
        return "setting.html";
    }

    @PostMapping("/my/{userId}/profile-image")
    @PreAuthorize("isAuthenticated()")
    public String uploadProfileImage(@PathVariable Long userId, 
                                    @RequestParam String image) {
        
        userService.updateUserProfileImage(userId, image);
        return "redirect:/setting";
    }

    @GetMapping("/verify-password")
    @PreAuthorize("isAuthenticated()")
    public String verifyPasswordForm(Model model, 
                                @RequestParam(required = false) String nextAction) {
        model.addAttribute("nextAction", nextAction);
        return "verify-password.html";
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
        CustomUser user = (CustomUser) auth.getPrincipal();

        try {
            String verifiedToken = userService.verifyPassword(user.id, email, password);

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

    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePasswordForm() {
        return "change-password.html";
    }

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

        CustomUser user = (CustomUser) auth.getPrincipal();

        try {
            userService.changePassword(user.id, newPassword);
            
            // 사용 완료된 토큰 쿠키 삭제
            clearVerifiedTokenCookie(response);
            
            redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/setting";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-password";
        }
    }

    @GetMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteAccountForm() {
        return "delete-account.html";
    }

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

        CustomUser user = (CustomUser) auth.getPrincipal();

        try {
            userService.deleteAccount(user.id);
            
            // 모든 인증 쿠키 삭제
            clearAllAuthCookies(response);
            
            redirectAttributes.addFlashAttribute("successMessage", "계정이 성공적으로 삭제되었습니다.");
            return "redirect:/list";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/verify-password?nextAction=delete-account";
        }
    }

    // Private Helper Methods

    // JWT 쿠키 설정
    private void setJwtCookie(HttpServletResponse response) {
        var jwt = JwtUtil.createToken(SecurityContextHolder.getContext().getAuthentication());
        var cookie = new Cookie("jwt", jwt);
        cookie.setMaxAge(3600); // 1시간
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    // JWT 쿠키를 삭제
    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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

    // 모든 인증 관련 쿠키를 삭제 (JWT + verified_token).
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

    // 본인 인증 토큰을 검증합니다.
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