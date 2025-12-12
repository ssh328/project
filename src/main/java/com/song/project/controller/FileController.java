package com.song.project.controller;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.song.project.security.CustomUser;
import com.song.project.service.PostService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FileController {
    private final PostService postService;
    private final S3Service s3Service;

    // Presigned URL 생성
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    String getURL(@RequestParam String filename) {

        // 확장자 추출
        String extension = "";

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex != -1) {
            extension = filename.substring(dotIndex);
        }

        // UUID로 unique 파일명 생성
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // S3에 저장될 경로 구성
        String key = "project/" + uniqueFileName;

        // Presigned URL 생성
        String result = s3Service.createPresignedUrl(key);

        return result;
    }

    // 이미지 삭제
    @DeleteMapping("/delete-image")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    ResponseEntity<String> deleteImages(@RequestParam Long imageId, Authentication auth) {

        CustomUser user = (CustomUser) auth.getPrincipal();
        postService.deleteImage(imageId, user.id);
        return ResponseEntity.ok("삭제완료");
    }
}
