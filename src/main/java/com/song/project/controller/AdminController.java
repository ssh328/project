package com.song.project.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.song.project.TalkjsService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final TalkjsService talkjsService;

    public AdminController(TalkjsService talkjsService) {
        this.talkjsService = talkjsService;
    }

    @PostMapping("/delete-conversation")
    public ResponseEntity<String> deleteConversation(
            @RequestBody Map<String, String> body) {

        System.out.println("=== /admin/delete-conversation 호출됨 ==="); // 1️⃣ 컨트롤러 진입 확인
        System.out.println("Request body: " + body); // 2️⃣ 클라이언트에서 넘어온 JSON 확인

        String conversationId = body.get("conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            System.out.println("conversationId가 없어서 400 반환"); // 3️⃣ conversationId 체크
            return ResponseEntity.badRequest().body("conversationId가 필요합니다.");
        }

        System.out.println("삭제 요청 conversationId: " + conversationId); // 4️⃣ 실제 삭제할 conversationId 확인

        boolean success = talkjsService.deleteConversation(conversationId);
        if (success) {
            return ResponseEntity.ok("삭제 완료");
        } else {
            System.out.println("삭제 실패, 500 반환"); // 7️⃣ 실패 로그
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("삭제 실패");
        }
    }
}

// 대화 삭제 기능 하다가 맘