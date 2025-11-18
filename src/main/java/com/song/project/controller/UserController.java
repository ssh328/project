package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.JwtUtil;
import com.song.project.dto.PostListDto;
import com.song.project.dto.UserProfileDto;
import com.song.project.entity.Likes;
import com.song.project.entity.Post;
import com.song.project.entity.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseCookie;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;
import com.song.project.service.PostViewCountService;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userrepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;

    @GetMapping("/register")
    String register() {
         return "register.html";
    }

    @PostMapping("/user-register")
    String addUser(@RequestParam String user_id,
                   @RequestParam String password,
                   @RequestParam String username,
                   @RequestParam String email,
                   RedirectAttributes redirectAttributes,
                   HttpServletResponse response) {

        User user = new User();
        user.setUser_id(user_id);
        user.setUsername(username);
        user.setEmail(email);
        var hash = passwordEncoder.encode(password);
        user.setPassword(hash);
        user.setDp("https://javaspringproject.s3.ap-northeast-2.amazonaws.com/project/default-profile-img.png");
        userrepository.save(user);

        var authToken = new UsernamePasswordAuthenticationToken(
            username, password
            );

        var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var jwt = JwtUtil.createToken(SecurityContextHolder.getContext().getAuthentication());

        var cookie = new Cookie("jwt", jwt);
        cookie.setMaxAge(3600); // 유효 1시간 = 3600
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        redirectAttributes.addFlashAttribute("successMessage", "환영합니다!");
        return "redirect:/list";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }

    @PostMapping("/login/jwt")
    @ResponseBody
    public Map<String, Object> loginJWT(@RequestBody Map<String, String> data,
                           HttpServletResponse response) {

        var authToken = new UsernamePasswordAuthenticationToken(
                data.get("username"), data.get("password")
        );

        var auth = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);

        var jwt = JwtUtil.createToken(SecurityContextHolder.getContext().getAuthentication());

        var cookie = new Cookie("jwt", jwt);
        cookie.setMaxAge(3600); // 유효 1시간 = 3600
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/logout/jwt")
    public String logoutJwt(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);

        return "redirect:/list"; // 로그아웃 후 이동할 페이지
    }

    @GetMapping("/my-page")
    @PreAuthorize("isAuthenticated()")
    public String myPage(Authentication auth) {
        CustomUser result = (CustomUser) auth.getPrincipal();
        System.out.println(result.user_id);
        return "mypage.html";
    }

    @GetMapping("/my-page/jwt")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    String myPageJWT(Authentication auth) {
        var user = (CustomUser) auth.getPrincipal();
        System.out.println(user);
        System.out.println(user.user_id);
        System.out.println(user.getAuthorities());
        System.out.println(user.id);
        return "마이페이지데이터";
    }

    @GetMapping("/my/posts")
    @PreAuthorize("isAuthenticated()")
    String getMyPosts(Model model, Authentication auth, 
    @RequestParam(defaultValue = "1") int page) {
        CustomUser user = (CustomUser) auth.getPrincipal();

        Page<Post> data = postRepository.findByUserIdOrderByIdDesc(
            user.id, PageRequest.of(page - 1, 20));

        Page<PostListDto> postDtos = data.map(PostListDto::from);

        // 조회수 합산용 처리
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());

        // Redis 조회수 가져오기
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        List<Long> likedPostIds = likeRepository.findByUserId(user.id).stream()
            .map(like -> like.getPost().getId())
            .collect(Collectors.toList());

        model.addAttribute("likedPostIds", likedPostIds);
        model.addAttribute("posts", postDtos);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", data.getTotalPages());
        model.addAttribute("viewCounts", redisViewCounts);
        
        return "mypost.html";
    }

    @GetMapping("/my/likes")
    @PreAuthorize("isAuthenticated()")
    String getMyLikes(Model model, Authentication auth, 
    @RequestParam(defaultValue = "1") int page) {
        CustomUser user = (CustomUser) auth.getPrincipal();

        Page<Likes> data = likeRepository.findByUserId(user.id, PageRequest.of(page - 1, 20));

        // likeRepository.findByUserId(user.id)로 해당 유저가 누른 Like 엔티티들을 가져오고, 
        // 그 리스트에서 각 Like가 참조하는 Post만 골라(map), null인 항목은 제거(filter), 최종적으로 List<Post>로 만든다(collect)
        // 결과: 사용자가 좋아요 누른 실제 게시물 목록 (List<Post>) 을 얻음
//        List<Post> likedPosts = likeRepository.findByUserId(user.id).stream()
//        .map(Likes::getPost)
//        .filter(Objects::nonNull)
//        .collect(Collectors.toList());
        Page<Post> postPage = data.map(Likes::getPost);
        Page<PostListDto> postDtos = postPage.map(PostListDto::from);

        // 조회수 합산용 처리
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());

        // Redis 조회수 가져오기
        Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

        List<Long> likedPostIds = likeRepository.findByUserId(user.id).stream()
        .map(like -> like.getPost().getId())
        .collect(Collectors.toList());
        
        model.addAttribute("posts", postDtos);
        model.addAttribute("likedPostIds", likedPostIds);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", data.getTotalPages());
        model.addAttribute("viewCounts", redisViewCounts);

        return "mylike.html";
    }

    @GetMapping("/setting")
    @PreAuthorize("isAuthenticated()")
    String setting (Model model, Authentication auth) {
        CustomUser loginUser = (CustomUser) auth.getPrincipal();

        User user = userrepository.findById(loginUser.id)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        UserProfileDto userDto = new UserProfileDto(user);
        
        model.addAttribute("currentUser", userDto);
        return "setting.html";
    }

    @PostMapping("/my/{userId}/profile-image")
    String uploadProfileImage (@PathVariable Long userId, @RequestParam String image) {

        User user = userrepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setDp(image);
        userrepository.save(user);

        return "redirect:/setting";
    }

    @GetMapping("/verify-password")
    @PreAuthorize("isAuthenticated()")
    String verifyPasswordForm(Model model, @RequestParam(required = false) String nextAction) {
        System.out.println("nextAction" + nextAction);
        model.addAttribute("nextAction", nextAction);
        return "verify-password.html";
    }

    // @PostMapping("/verify-password")
    // @PreAuthorize("isAuthenticated()")
    // String verifyPassword(@RequestParam String email, @RequestParam String password, Authentication auth,
    //                     RedirectAttributes redirectAttributes) {
    //     CustomUser loginUser = (CustomUser) auth.getPrincipal();
    //     User user = userrepository.findById(loginUser.id).orElseThrow();

    //     if (user.getEmail().equals(email) && passwordEncoder.matches(password, user.getPassword())) {
    //         redirectAttributes.addFlashAttribute("authenticated", true);
    //         return "redirect:/change-password";
    //     } else {
    //         redirectAttributes.addFlashAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
    //         return "redirect:/verify-password";
    //     }
    // }

    // POST /verify-password : 이메일/비밀번호 확인 -> HttpOnly cookie 발급 후 리다이렉트
    @PostMapping("/verify-password")
    @PreAuthorize("isAuthenticated()")
    String verifyPassword(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String nextAction,
            Authentication auth,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        CustomUser loginUser = (CustomUser) auth.getPrincipal();
        User user = userrepository.findById(loginUser.id).orElseThrow();

        if (!user.getEmail().equals(email) || !passwordEncoder.matches(password, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "이메일 또는 비밀번호가 올바르지 않습니다.");

            String redirectUrl;
            if ("delete-account".equals(nextAction)) {
                redirectUrl = "/verify-password?nextAction=delete-account";
            } else {
                redirectUrl = "/verify-password?nextAction=change-password";
            }

            return "redirect:" + redirectUrl;
        }

        // ✅ 임시 토큰 생성 (5분 유효)
        String verifiedToken = JwtUtil.createTemporaryToken(
                user.getUsername(),
                user.getId(),
                5 * 60 * 1000
        );

        // HttpOnly cookie 생성 (Secure, SameSite=Strict 권장)
        ResponseCookie cookie = ResponseCookie.from("verified_token", verifiedToken)
                .httpOnly(true)
                .secure(false) // 배포 시 HTTPS면 true; 로컬에서는 false로 테스트
                .path("/")    // 범위. 필요하면 세부 경로로 제한
                .maxAge(5 * 60) // 초 단위
                .sameSite("Strict")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String redirectUrl;
        if ("delete-account".equals(nextAction)) {
            redirectUrl = "/delete-account";
        } else {
            redirectUrl = "/change-password";
        }

        redirectAttributes.addFlashAttribute("successMessage", "본인 인증이 완료되었습니다.");
        return "redirect:" + redirectUrl;
    }

    @GetMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    String changePasswordForm() {
         return "change-password.html";
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    String changePassword(
            @CookieValue(value = "verified_token", required = false) String verifiedToken,
            @RequestParam String newPassword,
            Authentication auth,
            RedirectAttributes redirectAttributes,
            HttpServletResponse response
    ) {
        if (verifiedToken == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 인증이 필요합니다.");
            return "redirect:/verify-password";
        }

        // ✅ 1차 검증: 토큰이 위조/만료되지 않았는가?
        if (!JwtUtil.validateTemporaryToken(verifiedToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 만료된 인증입니다.");
            return "redirect:/verify-password";
        }

        // verify-token 검증
        // ✅ 2차 검증: 토큰의 용도(type)가 올바른가?
        var claims = JwtUtil.extractToken(verifiedToken);
        if (!"verify".equals(claims.get("type"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 인증 토큰입니다.");
            return "redirect:/verify-password";
        }

        CustomUser loginUser = (CustomUser) auth.getPrincipal();
        User user = userrepository.findById(loginUser.id).orElseThrow();

        user.setPassword(passwordEncoder.encode(newPassword));
        userrepository.save(user);

        // 사용 완료된 토큰 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("verified_token", "")
        .httpOnly(true)
        .secure(false) // 배포 시 HTTPS면 true
        .path("/")
        .maxAge(0) // 즉시 만료
        .sameSite("Strict")
        .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        redirectAttributes.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");

        return "redirect:/setting";
    }

    @GetMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    String deleteAccountForm() {
         return "delete-account.html";
    }

    @PostMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    String deleteAccount( @CookieValue(value = "verified_token", required = false) String verifiedToken, Authentication auth,
        RedirectAttributes redirectAttributes,
        HttpServletResponse response) {

        // ✅ 인증 토큰 검증
        if (verifiedToken == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "본인 인증이 필요합니다.");
            return "redirect:/verify-password?nextAction=delete-account";
        }

        if (!JwtUtil.validateTemporaryToken(verifiedToken)) {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 만료된 인증입니다.");
            return "redirect:/verify-password?nextAction=delete-account";
        }
    
        var claims = JwtUtil.extractToken(verifiedToken);
        if (!"verify".equals(claims.get("type"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 인증 토큰입니다.");
            return "redirect:/verify-password?nextAction=delete-account";
        }

        ResponseCookie clearJwt = ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();

        ResponseCookie deleteCookie = ResponseCookie.from("verified_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearJwt.toString());

        CustomUser loginUser = (CustomUser) auth.getPrincipal();
        User user = userrepository.findById(loginUser.id).orElseThrow();
        userrepository.delete(user);

    
        redirectAttributes.addFlashAttribute("successMessage", "계정이 성공적으로 삭제되었습니다.");
        return "redirect:/list";
    }

}

// class UserDto {
//     public String username;
//     public String user_id;

//     // constructor 사용
//     UserDto(String a, String b) {
//         this.username = a;
//         this.user_id = b;
//     }
// }