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

import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostListDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.dto.RecommendedPostDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.entity.PostStatus;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostImageRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
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

    /**
     * 게시물 목록을 필터링 및 페이지네이션과 함께 조회
     * @param category 카테고리 필터 (선택적)
     * @param startPrice 최소 가격 필터 (선택적)
     * @param endPrice 최대 가격 필터 (선택적)
     * @param status 게시글 상태 필터 (선택적)
     * @param page 페이지 번호 (기본값: 1)
     * @param sortBy 정렬 기준 ("hottest": 인기순, 그 외: 최신순)
     * @return 게시물 목록 페이지
     */
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

    /**
     * 게시물을 검색어로 조회
     * @param searchText 검색어
     * @param page 페이지 번호 (기본값: 1)
     * @return 검색 결과 페이지
     */
    public Page<PostListDto> searchPosts(String searchText, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        Page<Post> data = postRepository.fullTextSearchWithPaging(searchText, pageRequest);
        return data.map(PostListDto::from);
    }

    /**
     * 게시물 목록 결과 생성 (좋아요 여부, 조회수 포함)
     * @param postDtos 게시물 목록 페이지
     * @param userId 현재 로그인한 사용자 ID (선택적)
     * @return 게시물 목록 결과
     */
    public PostListResult getPostListResult(Page<PostListDto> postDtos, Long userId) {
        List<Long> likedPostIds = getLikedPostIds(userId);
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);

        return new PostListResult(postDtos, likedPostIds, viewCounts);
    }

    /**
     * 검색 결과 생성 (좋아요 여부 포함)
     * @param postDtos 검색 결과 페이지
     * @param userId 현재 로그인한 사용자 ID (선택적)
     * @return 검색 결과
     */
    public SearchResult getSearchResult(Page<PostListDto> postDtos, Long userId) {
        List<Long> likedPostIds = getLikedPostIds(userId);
        return new SearchResult(postDtos, likedPostIds);
    }

    /**
     * 상태 문자열을 PostStatus enum으로 파싱
     * @param status 상태 문자열 (선택적)
     * @return 파싱 결과 (PostStatus, 선택된 상태 문자열)
     */
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

    /**
     * 현재 로그인 사용자가 좋아요한 게시물 ID 목록 조회
     * @param userId 사용자 ID (null이면 빈 리스트 반환)
     * @return 좋아요한 게시물 ID 목록
     */
    public List<Long> getLikedPostIds(Long userId) {
        if (userId == null) {
            return List.of();
        }

        return likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());
    }

    /**
     * 여러 게시물에 대한 조회수 조회
     * @param postIds 게시물 ID 목록
     * @return 게시물 ID와 조회수를 담은 Map
     */
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    /**
     * 게시물 단건 조회 (없으면 예외 발생)
     * @param postId 게시물 ID
     * @return 게시물 엔티티
     * @throws NotFoundException 게시물을 찾을 수 없는 경우
     */
    public Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    /**
     * 게시물 상세 페이지 결과 조회
     * 조회수 증가 및 추천 게시글 포함
     * @param postId 게시물 ID
     * @param loginUserId 로그인한 사용자 ID (선택적)
     * @param viewToken 비로그인 사용자용 조회 토큰 (선택적)
     * @return 게시물 상세 결과
     * @throws NotFoundException 게시물을 찾을 수 없는 경우
     */
    public PostDetailResult getPostDetailResult(Long postId, Long loginUserId, String viewToken) {
        Post post = getPostOrThrow(postId);
        Long viewCount = incrementViewCount(postId, loginUserId, viewToken);
        recommendedPostService.addViewScore(postId);
        List<RecommendedPostDto> recommendedPosts = getRecommendedPostsByCategory(post);
        
        return new PostDetailResult(post, post.getUser().getId(), loginUserId, viewCount, recommendedPosts);
    }

    /**
     * 조회수 증가 및 현재 조회수 반환
     * @param postId 게시물 ID
     * @param loginUserId 로그인한 사용자 ID (선택적)
     * @param viewToken 비로그인 사용자용 조회 토큰 (선택적)
     * @return 증가된 조회수
     */
    public Long incrementViewCount(Long postId, Long loginUserId, String viewToken) {
        return postViewCountService.incrementAndGetViewCount(postId, loginUserId, viewToken);
    }

    /**
     * 카테고리 기반 추천 게시물 목록 조회
     * @param currentPost 현재 게시물
     * @return 추천 게시물 목록
     */
    @Transactional(readOnly = true)
    public List<RecommendedPostDto> getRecommendedPostsByCategory(Post currentPost) {
        // RecommendedPostService에서 이미 EntityGraph로 조회한 Post 객체를 재사용
        List<Post> recommendedPosts = recommendedPostService.recommendPopularByCategory(currentPost);

        return recommendedPosts.stream()
                .map(RecommendedPostDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 게시글 생성
     * @param userId 작성자 ID
     * @param dto 게시글 생성 정보
     * @return 생성된 게시글
     * @throws NotFoundException 사용자를 찾을 수 없는 경우
     * @throws BadRequestException 게시글 생성 실패 시
     */
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
            log.info("게시글 생성 성공: postId={}, title={}, userId={}", 
                saved.getId(), saved.getTitle(), userId);
        } catch (DataIntegrityViolationException e) {
            log.error("게시물 생성 실패: userId={}, title={}", userId, dto.getTitle(), e);
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
                        log.error("이미지 저장 실패: postId={}, url={}", saved.getId(), trimmedUrl, e);
                        throw new BadRequestException("이미지 저장에 실패했습니다.");
                    }
                }
            }
            log.debug("게시글 이미지 저장 완료: postId={}", saved.getId());
        }

        return saved;
    }

    /**
     * 게시글 수정
     * 작성자만 수정 가능
     * @param dto 게시글 수정 정보
     * @param userId 현재 로그인한 사용자 ID
     * @return 수정된 게시글
     * @throws NotFoundException 게시글을 찾을 수 없는 경우
     * @throws UnauthorizedException 작성자가 아닌 경우
     * @throws BadRequestException 게시글 수정 실패 시
     */
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

        Post updated;
        try {
            updated = postRepository.save(post);
            log.info("게시글 수정 성공: postId={}, title={}, userId={}", 
                dto.getPostId(), dto.getTitle(), userId);
        } catch (DataIntegrityViolationException e) {
            log.error("게시물 수정 실패: postId={}, userId={}", dto.getPostId(), userId, e);
            throw new BadRequestException("게시물 수정에 실패했습니다.");
        }

        // 새로 업로드된 이미지 처리 (기존 이미지는 유지, 삭제는 별도 API)
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String[] urls = dto.getImage().split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.isEmpty()) {
                    try {
                        PostImage postImage = new PostImage();
                        postImage.setImgUrl(trimmedUrl);
                        postImage.setPost(updated);
                        postImageRepository.save(postImage);
                    } catch (Exception e) {
                        throw new BadRequestException("이미지 저장에 실패했습니다.");
                    }
                }
            }
        }

        return updated;
    }

    /**
     * 게시글 삭제 공통 로직
     * S3 이미지 삭제, 좋아요 삭제, DB 삭제를 수행하는 공통 메서드
     * 일반 사용자용 deletePost()와 관리자용 AdminService.deletePostAsAdmin()에서 공통으로 사용
     * @param post 삭제할 Post 엔티티 (이미 조회된 상태여야 함)
     * @throws BadRequestException 게시물 삭제 실패 시
     */
    @Transactional
    public void deletePostInternal(Post post) {
        // S3에 저장된 게시글 이미지 삭제
        for (PostImage img : post.getImages()) {
            String key = s3Service.extractS3Key(img.getImgUrl());
            s3Service.deleteFile(key);
        }

        try {
            // 게시글에 달린 좋아요 삭제
            likeRepository.deleteAllByPostId(post.getId());
            
            // 게시글 엔티티 삭제
            postRepository.delete(post);
            
            log.info("게시글 삭제 성공: postId={}, title={}", 
                post.getId(), post.getTitle());
        } catch (Exception e) {
            log.error("게시물 삭제 실패: postId={}, title={}", post.getId(), post.getTitle(), e);
            throw new BadRequestException("게시물 삭제에 실패했습니다.");
        }
    }

    /**
     * 게시글 삭제
     * 작성자만 삭제 가능, S3 이미지도 함께 삭제
     * @param postId 삭제할 게시글 ID
     * @param userId 현재 로그인한 사용자 ID
     * @throws NotFoundException 게시글을 찾을 수 없는 경우
     * @throws ForbiddenException 작성자가 아닌 경우
     * @throws BadRequestException 게시글 삭제 실패 시
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시물을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        deletePostInternal(post);

    }

    /**
     * 게시글 이미지 단건 삭제
     * 작성자만 삭제 가능, S3 이미지도 함께 삭제
     * @param imageId 삭제할 이미지 ID
     * @param userId 현재 로그인한 사용자 ID
     * @throws NotFoundException 이미지 또는 게시글을 찾을 수 없는 경우
     * @throws ForbiddenException 작성자가 아닌 경우
     */
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

    /**
     * 게시글 상태 변경
     * 작성자만 변경 가능
     * @param postId 게시글 ID
     * @param dto 상태 변경 정보
     * @param userId 현재 로그인한 사용자 ID
     * @return 변경된 게시글 상태
     * @throws NotFoundException 게시글을 찾을 수 없는 경우
     * @throws ForbiddenException 작성자가 아닌 경우
     */
    public PostStatus updateStatus(Long postId, PostStatusUpdateDto dto, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 상태를 변경할 수 있습니다.");
        }

        PostStatus postStatus = PostStatus.valueOf(dto.getStatus().toUpperCase());
        post.setStatus(postStatus);
        postRepository.save(post);
        
        log.info("게시글 상태 변경: postId={}, status={}, userId={}", 
            postId, postStatus, userId);

        return postStatus;
    }

    // ===========================
    // 유틸리티
    // ===========================

    /**
     * 카테고리 목록 반환
     * @return 카테고리 목록
     */
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

    /**
     * 수정용 게시글 조회 (권한 체크 포함)
     * 작성자만 조회 가능
     * @param postId 게시글 ID
     * @param userId 현재 로그인한 사용자 ID
     * @return 게시글 엔티티
     * @throws NotFoundException 게시글을 찾을 수 없는 경우
     * @throws UnauthorizedException 작성자가 아닌 경우
     */
    public Post getPostForEdit(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }
        return post;
    }

    /**
     * 사용자명으로 게시글 목록 조회
     * @param username 사용자명
     * @param page 페이지 번호 (기본값: 1)
     * @return 게시글 페이지
     */
    public Page<Post> getPostsByUsername(String username, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        return postRepository.findByUser_Username(username, pageRequest);
    }

    /**
     * 게시물 목록 결과를 담는 DTO
     */
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

    /**
     * 검색 결과를 담는 DTO
     */
    @Getter
    public static class SearchResult {
        private Page<PostListDto> posts;
        private List<Long> likedPostIds;

        public SearchResult(Page<PostListDto> posts, List<Long> likedPostIds) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
        }
    }

    /**
     * 상태 파싱 결과를 담는 DTO
     */
    @Getter
    public static class PostStatusParseResult {
        private PostStatus postStatus;
        private String selectedStatus;

        public PostStatusParseResult(PostStatus postStatus, String selectedStatus) {
            this.postStatus = postStatus;
            this.selectedStatus = selectedStatus;
        }
    }

    /**
     * 게시물 상세 결과를 담는 DTO
     */
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
}
