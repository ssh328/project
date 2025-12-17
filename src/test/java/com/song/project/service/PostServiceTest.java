package com.song.project.service;

import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.entity.PostStatus;
import com.song.project.entity.User;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostImageRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private PostViewCountService postViewCountService;

    @Mock
    private RecommendedPostService recommendedPostService;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_Success() {
        // Given
        Long userId = 1L;
        PostCreateDto dto = new PostCreateDto();
        dto.setTitle("테스트 게시글");
        dto.setPrice(10000);
        dto.setCategory("디지털기기");
        dto.setBody("테스트 내용입니다.");
        dto.setImage("https://example.com/image1.jpg,https://example.com/image2.jpg");

        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Post savedPost = new Post();
        savedPost.setId(1L);
        savedPost.setTitle(dto.getTitle());
        savedPost.setPrice(dto.getPrice());
        savedPost.setCategory(dto.getCategory());
        savedPost.setBody(dto.getBody());
        savedPost.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postImageRepository.save(any(PostImage.class))).thenAnswer(invocation -> {
            PostImage image = invocation.getArgument(0);
            image.setId(1L);
            return image;
        });

        // When
        Post result = postService.createPost(userId, dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("테스트 게시글");
        verify(userRepository, times(1)).findById(userId);
        verify(postRepository, times(1)).save(any(Post.class));
        verify(postImageRepository, times(2)).save(any(PostImage.class));
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        PostUpdateDto dto = new PostUpdateDto();
        dto.setPostId(postId);
        dto.setTitle("수정된 제목");
        dto.setPrice(20000);
        dto.setCategory("생활가전");
        dto.setBody("수정된 내용입니다.");

        User user = new User();
        user.setId(userId);

        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setTitle("원래 제목");
        existingPost.setPrice(10000);
        existingPost.setUser(user);

        Post updatedPost = new Post();
        updatedPost.setId(postId);
        updatedPost.setTitle(dto.getTitle());
        updatedPost.setPrice(dto.getPrice());
        updatedPost.setCategory(dto.getCategory());
        updatedPost.setBody(dto.getBody());
        updatedPost.setUser(user);

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(updatedPost);

        // When
        Post result = postService.updatePost(dto, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("수정된 제목");
        assertThat(result.getPrice()).isEqualTo(20000);
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        User user = new User();
        user.setId(userId);

        PostImage image1 = new PostImage();
        image1.setId(1L);
        image1.setImgUrl("https://s3.amazonaws.com/bucket/key1.jpg");

        PostImage image2 = new PostImage();
        image2.setId(2L);
        image2.setImgUrl("https://s3.amazonaws.com/bucket/key2.jpg");

        List<PostImage> images = new ArrayList<>();
        images.add(image1);
        images.add(image2);

        Post post = new Post();
        post.setId(postId);
        post.setTitle("삭제할 게시글");
        post.setUser(user);
        post.setImages(images);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        doNothing().when(likeRepository).deleteAllByPostId(postId);
        doNothing().when(postRepository).delete(post);
        when(s3Service.extractS3Key(anyString())).thenReturn("key1.jpg", "key2.jpg");
        doNothing().when(s3Service).deleteFile(anyString());

        // When
        postService.deletePost(postId, userId);

        // Then
        verify(postRepository, times(1)).findById(postId);
        verify(s3Service, times(2)).extractS3Key(anyString());
        verify(s3Service, times(2)).deleteFile(anyString());
        verify(likeRepository, times(1)).deleteAllByPostId(postId);
        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("게시글 상태 변경 성공")
    void updateStatus_Success() {
        // Given
        Long postId = 1L;
        Long userId = 1L;

        PostStatusUpdateDto dto = new PostStatusUpdateDto();
        dto.setStatus("SOLD");

        User user = new User();
        user.setId(userId);

        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.ON_SALE);
        post.setUser(user);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            savedPost.setStatus(PostStatus.SOLD);
            return savedPost;
        });

        // When
        PostStatus result = postService.updateStatus(postId, dto, userId);

        // Then
        assertThat(result).isEqualTo(PostStatus.SOLD);
        verify(postRepository, times(1)).findById(postId);
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 조회 성공")
    void getPostOrThrow_Success() {
        // Given
        Long postId = 1L;

        Post post = new Post();
        post.setId(postId);
        post.setTitle("테스트 게시글");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        Post result = postService.getPostOrThrow(postId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("테스트 게시글");
        verify(postRepository, times(1)).findById(postId);
    }
}
