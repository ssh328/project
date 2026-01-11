package com.song.project.controller;

import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.song.project.exception.BadRequestException;
import com.song.project.security.JwtUtil;
import com.song.project.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 관련 API를 제공하는 컨트롤러
 * 회원가입, 로그인, 로그아웃 처리
 */
@Slf4j
@Tag(name = "인증 API", description = "로그인/로그아웃 관련 API")
@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final AuthService authService;

    /**
     * 회원가입 페이지 조회
     */
    @GetMapping("/register")
    public String register() {
        return "auth/register.html";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/user-register")
    public String addUser(@RequestParam String user_id,
                        @RequestParam String password,
                        @RequestParam String username,
                        @RequestParam String email,
                        RedirectAttributes redirectAttributes,
                        HttpServletResponse response) {
        try {
            authService.register(user_id, password, username, email);
            
            // 자동 로그인 처리
            var authToken = new UsernamePasswordAuthenticationToken(username, password);
            var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            setJwtCookie(response);
    
            log.info("회원가입 및 자동 로그인 성공: userId={}, username={}", user_id, username);
            redirectAttributes.addFlashAttribute("successMessage", "환영합니다!");
            return "redirect:/post/list";
        } catch (BadRequestException e) {
            log.warn("회원가입 실패: userId={}, reason={}", user_id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    /**
     * 로그인 페이지 조회
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login.html";
    }

    /**
     * 로그인 처리
     */
    @Operation(summary = "로그인", description = "사용자 로그인을 처리하고 JWT 쿠키를 발급합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login/jwt")
    @ResponseBody
    public Map<String, Object> loginJWT(
        @RequestBody Map<String, String> data,
        HttpServletResponse response) {
        try {
            // 인증 처리
            var authToken = new UsernamePasswordAuthenticationToken(
                data.get("username"), data.get("password"));
            var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
    
            // JWT 쿠키 설정
            String token = JwtUtil.createToken(auth);
            setJwtCookie(response);
            
            String username = data.get("username");
            log.info("로그인 성공: username={}", username);
            return Map.of("success", true, "accessToken", token);
        } catch (BadCredentialsException e) {
            String username = data.get("username");
            log.warn("로그인 실패: username={}, reason=인증 정보 불일치", username);
            return Map.of("success", false, "message", "아이디 또는 비밀번호가 일치하지 않습니다.");
        } catch (Exception e) {
            String username = data.get("username");
            log.error("로그인 예외 발생: username={}", username, e);
            return Map.of("success", false, "message", "로그인에 실패했습니다.");
        }
    }

    /**
     * 로그아웃 처리
     */
    @PostMapping("/logout/jwt")
    public String logoutJWT(HttpServletResponse response) {
        clearJwtCookie(response);
        log.info("로그아웃 성공");
        return "redirect:/post/list";
    }

    // ===========================
    // 헬퍼 메서드
    // ===========================

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
}
