package com.song.project.controller;

import com.song.project.security.CustomUser;
import com.song.project.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * 좋아요 관련 API를 제공하는 컨트롤러
 */
@Tag(name = "좋아요 API", description = "게시글 좋아요 관련 API")
@Controller
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @Operation(summary = "좋아요 토글", description = "게시글에 좋아요를 추가하거나 제거합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성공"),
        @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @PostMapping("/like/{postId}")
    @ResponseBody
    public Map<String, Object> toggleLike(
        @PathVariable Long postId, 
        Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("loggedIn", false, "message", "로그인이 필요합니다.");
        }

        CustomUser user = (CustomUser) auth.getPrincipal();
        return likeService.toggleLike(postId, user);
    }
}