package com.song.project.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.song.project.controller.S3Service;
import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostListDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.dto.RecommendedPostDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.post.PostStatus;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostImageRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;
    private final RecommendedPostService recommendedPostService;
    private final S3Service s3Service;

    // ===========================
    // 목록 / 검색
    // ===========================

    // 게시물 목록 조회
    public Page<PostListDto> getPosts(String category,
                                      Integer startPrice,
                                      Integer endPrice,
                                      PostStatus status,
                                      int page,
                                      String sortBy) {

        Sort sort = Sort.by(Sort.Direction.DESC, "created");
        if ("hottest".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "likeCnt");
        }

        PageRequest pageRequest = PageRequest.of(page - 1, 20, sort);
        Page<Post> data = postRepository.findWithFilter(category, startPrice, endPrice, status, pageRequest);
        return data.map(PostListDto::from);
    }

    // 게시물 검색 조회
    public Page<PostListDto> searchPosts(String searchText, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        Page<Post> data = postRepository.fullTextSearchWithPaging(searchText, pageRequest);
        return data.map(PostListDto::from);
    }

    // 현재 로그인 사용자가 좋아요한 게시물 ID 목록
    public List<Long> getLikedPostIds(Long userId) {
        if (userId == null) {
            return List.of();
        }

        return likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());
    }

    // 여러 게시물에 대한 Redis 조회수 조회
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    // ===========================
    // 상세 / 조회수 / 추천
    // ===========================

    // 게시물 단건 조회 (없으면 예외)
    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    // 조회수 증가 및 현재 조회수 반환
    public Long incrementViewCount(Long postId, Long loginUserId, String viewToken) {
        return postViewCountService.incrementAndGetViewCount(postId, loginUserId, viewToken);
    }

    // 카테고리 기반 추천 게시물 목록
    public List<RecommendedPostDto> getRecommendedPostsByCategory(Long currentPostId) {
        // RecommendedPostService에서 이미 EntityGraph로 조회한 Post 객체를 재사용
        List<Post> recommendedPosts = recommendedPostService.recommendPopularByCategory(currentPostId);

        return recommendedPosts.stream()
                .map(RecommendedPostDto::from)
                .collect(Collectors.toList());
    }

    // ===========================
    // 생성 / 수정 / 삭제
    // ===========================

    // 게시글 생성
    public Post createPost(Long userId, PostCreateDto dto) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setPrice(dto.getPrice());
        post.setCategory(dto.getCategory());
        post.setBody(dto.getBody());
        post.setUser(user);

        Post saved = postRepository.save(post);

        // 이미지 저장
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String[] urls = dto.getImage().split(",");
            for (String url : urls) {
                PostImage postImage = new PostImage();
                postImage.setImgUrl(url.trim());
                postImage.setPost(saved);
                postImageRepository.save(postImage);
            }
        }

        return saved;
    }

    // 게시글 수정
    public Post updatePost(PostUpdateDto dto, Long userId) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }

        post.setTitle(dto.getTitle());
        post.setPrice(dto.getPrice());
        post.setCategory(dto.getCategory());
        post.setBody(dto.getBody());

        Post updated = postRepository.save(post);

        // 새로 업로드된 이미지 처리 (기존 이미지는 유지, 삭제는 별도 API)
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String[] urls = dto.getImage().split(",");
            for (String url : urls) {
                PostImage postImage = new PostImage();
                postImage.setImgUrl(url.trim());
                postImage.setPost(updated);
                postImageRepository.save(postImage);
            }
        }

        return updated;
    }

    // 게시글 삭제
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시물을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        // S3 이미지 삭제
        for (PostImage img : post.getImages()) {
            String key = s3Service.extractS3Key(img.getImgUrl());
            s3Service.deleteFile(key);
        }

        postRepository.delete(post);
    }

    // 게시글 이미지 단건 삭제
    public void deleteImage(Long imageId, Long userId) {
        PostImage img = postImageRepository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("이미지를 찾을 수 없습니다."));

        Post post = img.getPost();
        if (post == null) {
            throw new NotFoundException("게시글을 찾을 수 없습니다.");
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글의 이미지만 삭제할 수 있습니다.");
        }

        String key = s3Service.extractS3Key(img.getImgUrl());
        s3Service.deleteFile(key);

        postImageRepository.delete(img);
    }

    // 게시글 상태 변경
    public PostStatus updateStatus(Long postId, PostStatusUpdateDto dto, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 상태를 변경할 수 있습니다.");
        }

        PostStatus postStatus = PostStatus.valueOf(dto.getStatus().toUpperCase());
        post.setStatus(postStatus);
        postRepository.save(post);

        return postStatus;
    }

    // ===========================
    // 유틸리티
    // ===========================

    // 카테고리 목록 반환
    public List<String> getCategories() {
        return List.of(
            "디지털기기", "생활가전", "가구/인테리어", "생활/주방",
            "유아동", "유아도서", "여성의류", "여성잡화",
            "남성패션/잡화", "뷰티/미용", "스포츠/레저",
            "취미/게임/음반", "도서", "티켓/교환권",
            "가공식품", "건강기능식품", "반려동물용품",
            "식물", "기타 중고물품"
        );
    }

    // 수정용 게시글 조회 (권한 체크 포함)
    public Post getPostForEdit(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }
        return post;
    }

    // 사용자명으로 게시글 목록 조회
    public Page<Post> getPostsByUsername(String username, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        return postRepository.findByUser_Username(username, pageRequest);
    }
}
