package com.song.project.service.post;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.dto.PostListDto;
import com.song.project.dto.RecommendedPostDto;
import com.song.project.entity.Post;
import com.song.project.entity.PostStatus;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostRepository;
import com.song.project.service.PostViewCountService;
import com.song.project.service.RecommendedPostService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 관련 조회(Read) 로직을 전담하는 서비스
 */
@Service
@RequiredArgsConstructor
public class PostQueryService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final PostViewCountService postViewCountService;
    private final RecommendedPostService recommendedPostService;

    /**
     * 게시물 목록을 필터링 및 페이지네이션과 함께 조회
     */
    @Transactional(readOnly = true)
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
     */
    @Transactional(readOnly = true)
    public Page<PostListDto> searchPosts(String searchText, int page) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20);
        Page<Post> data = postRepository.fullTextSearchActiveWithPaging(searchText, pageRequest);
        return data.map(PostListDto::from);
    }

    /**
     * 게시물 목록 결과 생성 (좋아요 여부, 조회수 포함)
     */
    @Transactional(readOnly = true)
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
     */
    @Transactional(readOnly = true)
    public SearchResult getSearchResult(Page<PostListDto> postDtos, Long userId) {
        List<Long> likedPostIds = getLikedPostIds(userId);
        List<Long> postIds = postDtos.stream()
                .map(PostListDto::getId)
                .collect(Collectors.toList());
        Map<Long, Long> viewCounts = getViewCountsForPosts(postIds);
        
        return new SearchResult(postDtos, likedPostIds, viewCounts);
    }

    /**
     * 상태 문자열을 PostStatus enum으로 파싱
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
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedPostIds(Long userId) {
        if (userId == null) {
            return List.of();
        }

        return likeRepository.findByUserId(userId).stream()
                .filter(like -> like.getPost() != null && !like.getPost().isDeleted())
                .map(like -> like.getPost().getId())
                .collect(Collectors.toList());
    }

    /**
     * 여러 게시물에 대한 조회수 조회
     */
    public Map<Long, Long> getViewCountsForPosts(List<Long> postIds) {
        return postViewCountService.getViewCountsForPosts(postIds);
    }

    /**
     * 게시물 단건 조회 (없으면 예외 발생)
     */
    @Transactional(readOnly = true)
    public Post getPostOrThrow(Long postId) {
        return postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    /**
     * 게시물 상세 페이지 결과 조회
     */
    @Transactional(readOnly = true)
    public PostDetailResult getPostDetailResult(Long postId, Long loginUserId, String viewToken) {
        Post post = getPostOrThrow(postId);
        Long viewCount = incrementViewCount(postId, loginUserId, viewToken);
        recommendedPostService.addViewScore(postId);
        List<RecommendedPostDto> recommendedPosts = getRecommendedPostsByCategory(post);
        
        return new PostDetailResult(post, post.getUser().getId(), loginUserId, viewCount, recommendedPosts);
    }

    /**
     * 조회수 증가 및 현재 조회수 반환
     */
    public Long incrementViewCount(Long postId, Long loginUserId, String viewToken) {
        return postViewCountService.incrementAndGetViewCount(postId, loginUserId, viewToken);
    }

    /**
     * 카테고리 기반 추천 게시물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RecommendedPostDto> getRecommendedPostsByCategory(Post currentPost) {
        List<Post> recommendedPosts = recommendedPostService.recommendPopularByCategory(currentPost);

        return recommendedPosts.stream()
                .map(RecommendedPostDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 목록 반환
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
     */
    @Transactional(readOnly = true)
    public Post getPostForEdit(Long postId, Long userId) {
        Post post = getPostOrThrow(postId);
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
        }
        return post;
    }

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
        private Map<Long, Long> viewCounts;

        public SearchResult(Page<PostListDto> posts, List<Long> likedPostIds, Map<Long, Long> viewCounts) {
            this.posts = posts;
            this.likedPostIds = likedPostIds;
            this.viewCounts = viewCounts;
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
}
