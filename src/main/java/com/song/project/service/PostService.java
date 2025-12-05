package com.song.project.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.controller.S3Service;
import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostListDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.dto.RecommendedPostDto;
import com.song.project.dto.UserProfileDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.entity.Review;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.post.PostStatus;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostImageRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.ReviewRepository;
import com.song.project.repository.UserRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final PostViewCountService postViewCountService;
    private final RecommendedPostService recommendedPostService;
    private final S3Service s3Service;

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

    // 게시물 목록 결과
    public PostListResult getPostListResult(Page<PostListDto> postDtos, Long userId) {
        List<Long> likedPostIds = getLikedPostIds(userId);
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);

        return new PostListResult(postDtos, likedPostIds, viewCounts);
    }

    // 검색 결과
    public SearchResult getSearchResult(Page<PostListDto> postDtos, Long userId) {
        List<Long> likedPostIds = getLikedPostIds(userId);
        return new SearchResult(postDtos, likedPostIds);
    }

    // 상태 파라미터 파싱
    public PostStatusParseResult getPostStatusParseResult(String status) {
        PostStatus postStatus = null;
        String selectedStatus = null;
        if (status != null && !status.isEmpty() && !"null".equals(status)) {
            try {
                postStatus = PostStatus.valueOf(status.toUpperCase());
                selectedStatus = status;
            } catch (IllegalArgumentException e) {
                // 잘못된 상태 값은 무시
            }
        }
        return new PostStatusParseResult(postStatus, selectedStatus);
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

    // 게시물 단건 조회 (없으면 예외)
    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    // 상세 페이지 결과 조회
    public PostDetailResult getPostDetailResult(Long postId, Long loginUserId, String viewToken) {
        Post post = getPostOrThrow(postId);
        Long viewCount = incrementViewCount(postId, loginUserId, viewToken);
        recommendedPostService.addViewScore(postId);
        List<RecommendedPostDto> recommendedPosts = getRecommendedPostsByCategory(post);
        
        return new PostDetailResult(post, post.getUser().getId(), loginUserId, viewCount, recommendedPosts);
    }

    // 조회수 증가 및 현재 조회수 반환
    public Long incrementViewCount(Long postId, Long loginUserId, String viewToken) {
        return postViewCountService.incrementAndGetViewCount(postId, loginUserId, viewToken);
    }

    // 카테고리 기반 추천 게시물 목록
    @Transactional(readOnly = true)
    public List<RecommendedPostDto> getRecommendedPostsByCategory(Post currentPost) {
        // RecommendedPostService에서 이미 EntityGraph로 조회한 Post 객체를 재사용
        List<Post> recommendedPosts = recommendedPostService.recommendPopularByCategory(currentPost);

        return recommendedPosts.stream()
                .map(RecommendedPostDto::from)
                .collect(Collectors.toList());
    }

    // 게시글 생성
    @Transactional
    public Post createPost(Long userId, PostCreateDto dto) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Post post = new Post();
        post.setTitle(dto.getTitle());
        post.setPrice(dto.getPrice());
        post.setCategory(dto.getCategory());
        post.setBody(dto.getBody());
        post.setUser(user);

        Post saved;
        try {
            saved = postRepository.save(post);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("게시물 생성에 실패했습니다.");
        }

        // 이미지 저장
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String[] urls = dto.getImage().split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.isEmpty()) {
                    try {
                        PostImage postImage = new PostImage();
                        postImage.setImgUrl(trimmedUrl);
                        postImage.setPost(saved);
                        postImageRepository.save(postImage);
                    } catch (Exception e) {
                        throw new BadRequestException("이미지 저장에 실패했습니다.");
                    }
                }
            }
        }

        return saved;
    }

    // 게시글 수정
    @Transactional
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
    @Transactional
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
        
        likeRepository.deleteAllByPostId(postId);

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

        // 프로필 페이지 결과
    public ProfileResult getProfileResult(String username, int postPage, int reviewPage, Long loginUserId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        UserProfileDto userDto = new UserProfileDto(user);

        Page<Post> posts = getPostsByUsername(username, postPage);
        Page<PostListDto> postDtos = posts.map(PostListDto::from);

        List<Long> postIds = postDtos.stream()
            .map(PostListDto::getId)
            .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);

        PageRequest reviewPageRequest = PageRequest.of(
            reviewPage - 1,
            3,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Review> reviews = reviewRepository.findByTargetUser_Id(
                userDto.getId(), reviewPageRequest);

        List<Long> likedPostIds = getLikedPostIds(user.getId());

        return new ProfileResult(userDto,
                                 postDtos, 
                                 reviews, 
                                 likedPostIds, 
                                 posts.getTotalPages(), 
                                 reviews.getTotalPages(), 
                                 loginUserId, 
                                 viewCounts);
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

    // DTO 클래스
    @Getter
    public static class PostListResult {
        private Page<PostListDto> posts;
        private List<Long> likedPostIds;
        private Map<Long, Long> viewCounts;

        public PostListResult(Page<PostListDto> posts, List<Long> likedPostIds, Map<Long, Long> viewCounts) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
            this.viewCounts = viewCounts;
        }
    }

    @Getter
    public static class SearchResult {
        private Page<PostListDto> posts;
        private List<Long> likedPostIds;

        public SearchResult(Page<PostListDto> posts, List<Long> likedPostIds) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
        }
    }

    @Getter
    public static class PostStatusParseResult {
        private PostStatus postStatus;
        private String selectedStatus;

        public PostStatusParseResult(PostStatus postStatus, String selectedStatus) {
            this.postStatus = postStatus;
            this.selectedStatus = selectedStatus;
        }
    }

    @Getter
    public static class PostDetailResult {
        private Post post;
        private Long postWriterId;
        private Long loginUserId;
        private Long viewCount;
        private List<RecommendedPostDto> recommendedPosts;

        public PostDetailResult(Post post, 
                                Long postWriterId, 
                                Long loginUserId, 
                                Long viewCount, 
                                List<RecommendedPostDto> recommendedPosts) {
            this.post = post;
            this.postWriterId = postWriterId;
            this.loginUserId = loginUserId;
            this.viewCount = viewCount;
            this.recommendedPosts = recommendedPosts;
        }
    }

    @Getter
    public static class ProfileResult {
        private UserProfileDto user;
        private Page<PostListDto> posts;
        private Page<Review> reviews;
        private List<Long> likedPostIds;
        private int postTotalPages;
        private int reviewTotalPages;
        private Long loginUserId;
        private Map<Long, Long> viewCounts;

        public ProfileResult(UserProfileDto user, 
                             Page<PostListDto> posts, 
                             Page<Review> reviews, 
                             List<Long> likedPostIds, 
                             int postTotalPages, 
                             int reviewTotalPages, 
                             Long loginUserId, 
                             Map<Long, Long> viewCounts) {
            this.user = user;
            this.posts = posts;
            this.reviews = reviews;
            this.likedPostIds = likedPostIds;
            this.postTotalPages = postTotalPages;
            this.reviewTotalPages = reviewTotalPages;
            this.loginUserId = loginUserId;
            this.viewCounts = viewCounts;
        }
    }
}
