package com.song.project.controller;
import com.song.project.entity.User;
import com.song.project.repository.UserRepository;
import com.song.project.repository.PostRepository;
import com.song.project.security.CustomUser;
import com.song.project.service.DirectDealService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 채팅 관련 API를 제공하는 컨트롤러
 * 채팅 페이지 조회
 * 유저 정보 조회
 */
@Tag(name = "채팅 API", description = "채팅 관련 API")
@CrossOrigin("*")
@Controller
public class ChatController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    DirectDealService directDealService;

    @Value("${talkjs.appId}")
    private String talkjsAppId;

    /**
     * 채팅 페이지 조회
     */
    @GetMapping("/chat")
    String chat(Model model,
                @RequestParam(required = false) Long postWriterId,
                @RequestParam(required = false) Long postId,
                Authentication auth) {

        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();

            model.addAttribute("userId", user.id);

            if (postWriterId != null) {
                model.addAttribute("postWriterId", postWriterId);
            } else {
                model.addAttribute("postWriterId", null);
            }

            if (postId != null) {
                model.addAttribute("postId", postId);
            } else {
                model.addAttribute("postId", null);
            }

            // "채팅 시작"을 직거래 구매자 후보로 기록 (postId가 전달된 경우에만)
            try {
                directDealService.recordChatCandidate(postId, user.id, postWriterId);
            } catch (Exception ignored) {
                // 채팅 페이지 진입은 항상 허용하되, 후보 기록만 실패할 수 있음
            }
        }

        model.addAttribute("talkjsAppId", talkjsAppId);
        
        return "chat/chat.html";
    }

    /**
     * 유저 정보 조회
     */
    @Operation(summary = "유저 정보 조회", description = "사용자 ID로 유저 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "사용자를 찾을 수 없습니다.")
    })
    @GetMapping(value = "/getUser")
    @ResponseBody
    public ResponseEntity<UserDto> getUser(
        @RequestParam(required = false) Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserDto userDto = new UserDto(user);
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    @Getter
    public static class UserDto {
        private Long id;
        private String username;
        private String user_id;
        private String email;
        private String dp;
        private String role;

        public UserDto(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.user_id = user.getUser_id();
            this.email = user.getEmail();
            this.dp = user.getDp();
            this.role = user.getRole();
        }
    }
}