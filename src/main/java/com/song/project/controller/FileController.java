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
import com.song.project.service.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@Tag(name = "파일 API", description = "파일 업로드/삭제 관련 API")
@Controller
@RequiredArgsConstructor
public class FileController {
    private final PostService postService;
    private final S3Service s3Service;

    @Operation(summary = "Presigned URL 생성", description = "S3에 파일을 업로드하기 위한 Presigned URL을 생성합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL 생성 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    String getURL(
        @RequestParam String filename) {

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

    @Operation(summary = "이미지 삭제", description = "이미지를 삭제합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @DeleteMapping("/delete-image")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    ResponseEntity<String> deleteImages(
        @RequestParam Long imageId, 
        Authentication auth) {

        CustomUser user = (CustomUser) auth.getPrincipal();
        postService.deleteImage(imageId, user.id);
        return ResponseEntity.ok("삭제완료");
    }
}
