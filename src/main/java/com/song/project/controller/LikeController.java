package com.song.project.controller;

import com.song.project.CustomUser;
import com.song.project.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/like/{postId}")
    @ResponseBody
    public Map<String, Object> toggleLike(@PathVariable Long postId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("loggedIn", false, "message", "로그인이 필요합니다.");
        }

        CustomUser user = (CustomUser) auth.getPrincipal();
        return likeService.toggleLike(postId, user);
    }
}