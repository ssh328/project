package com.song.project.controller;

import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.entity.PostStatus;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.service.post.PostCommandService;
import com.song.project.service.post.PostQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PostController.class, 
            excludeAutoConfiguration = {
                SecurityAutoConfiguration.class
            })
@DisplayName("PostController 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostQueryService postQueryService;

    @MockBean
    private PostCommandService postCommandService;

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() throws Exception {
        // Given
        Long postId = 1L;
        
        // Security를 제외했기 때문에 auth가 null이고, getUserId()가 null을 반환
        doNothing().when(postCommandService).deletePost(eq(postId), isNull());

        // When & Then
        mockMvc.perform(delete("/post/delete")
                        .param("id", String.valueOf(postId)))
                .andExpect(status().isOk())
                .andExpect(content().string("삭제완료"));

        verify(postCommandService, times(1)).deletePost(eq(postId), isNull());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 게시글 없음")
    void deletePost_NotFound() throws Exception {
        // Given
        Long postId = 999L;
        
        doThrow(new NotFoundException("게시글을 찾을 수 없습니다."))
                .when(postCommandService).deletePost(eq(postId), isNull());

        // When & Then
        mockMvc.perform(delete("/post/delete")
                        .param("id", String.valueOf(postId)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("게시글을 찾을 수 없습니다."));

        verify(postCommandService, times(1)).deletePost(eq(postId), isNull());
    }

    @Test
    @DisplayName("게시글 삭제 실패 - 권한 없음")
    void deletePost_Forbidden() throws Exception {
        // Given
        Long postId = 1L;
        
        doThrow(new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다."))
                .when(postCommandService).deletePost(eq(postId), isNull());

        // When & Then
        mockMvc.perform(delete("/post/delete")
                        .param("id", String.valueOf(postId)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("본인이 작성한 게시글만 삭제할 수 있습니다."));

        verify(postCommandService, times(1)).deletePost(eq(postId), isNull());
    }

    @Test
    @DisplayName("게시글 상태 변경 성공")
    void updateStatus_Success() throws Exception {
        // Given
        Long postId = 1L;
        
        when(postCommandService.updateStatus(eq(postId), any(PostStatusUpdateDto.class), isNull()))
                .thenReturn(PostStatus.SOLD);

        // When & Then
        mockMvc.perform(patch("/post/{id}/status", postId)
                        .param("status", "SOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상태가 변경되었습니다."))
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.statusDescription").value("판매완료"));

        verify(postCommandService, times(1)).updateStatus(eq(postId), any(PostStatusUpdateDto.class), isNull());
    }

    @Test
    @DisplayName("게시글 상태 변경 실패 - 잘못된 상태 값")
    void updateStatus_InvalidStatus() throws Exception {
        // Given
        Long postId = 1L;

        // When & Then - 유효성 검사 실패 (빈 값)
        mockMvc.perform(patch("/post/{id}/status", postId)
                        .param("status", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(postCommandService, never()).updateStatus(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("게시글 상태 변경 실패 - 잘못된 상태 형식")
    void updateStatus_InvalidStatusFormat() throws Exception {
        // Given
        Long postId = 1L;

        // When & Then - 유효성 검사 실패 (잘못된 형식)
        mockMvc.perform(patch("/post/{id}/status", postId)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(postCommandService, never()).updateStatus(anyLong(), any(), anyLong());
    }
}
