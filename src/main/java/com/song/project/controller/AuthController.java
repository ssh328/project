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

import com.song.project.JwtUtil;
import com.song.project.exception.BadRequestException;
import com.song.project.service.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final AuthService authService;

    @GetMapping("/register")
    public String register() {
        return "auth/register.html";
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
            authService.register(user_id, password, username, email);
            
            // 자동 로그인 처리
            var authToken = new UsernamePasswordAuthenticationToken(username, password);
            var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            setJwtCookie(response);
    
            redirectAttributes.addFlashAttribute("successMessage", "환영합니다!");
            return "redirect:/post/list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login.html";
    }

    @PostMapping("/login/jwt")
    @ResponseBody
    public Map<String, Object> loginJWT(@RequestBody Map<String, String> data,
                                        HttpServletResponse response) {
        
        try {
            // 인증 처리
            var authToken = new UsernamePasswordAuthenticationToken(
                data.get("username"), data.get("password"));
            var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);
    
            // JWT 쿠키 설정
            setJwtCookie(response);
    
            return Map.of("success", true);
        } catch (BadCredentialsException e) {
            return Map.of("success", false, "message", "아이디 또는 비밀번호가 일치하지 않습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", "로그인에 실패했습니다.");
        }
    }

    @PostMapping("/logout/jwt")
    public String logoutJWT(HttpServletResponse response) {
        clearJwtCookie(response);
        return "redirect:/post/list";
    }

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
