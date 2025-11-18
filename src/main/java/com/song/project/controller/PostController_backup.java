// package com.song.project.controller;

// import com.song.project.CustomUser;
// import com.song.project.dto.PostCreateDto;
// import com.song.project.dto.PostEditDto;
// import com.song.project.dto.PostListDto;
// import com.song.project.dto.PostStatusUpdateDto;
// import com.song.project.dto.PostUpdateDto;
// import com.song.project.dto.RecommendedPostDto;
// import com.song.project.dto.UserProfileDto;
// import com.song.project.entity.Post;
// import com.song.project.entity.PostImage;
// import com.song.project.entity.Review;
// import com.song.project.entity.User;
// import com.song.project.exception.UnauthorizedException;
// import com.song.project.post.PostStatus;
// import com.song.project.repository.LikeRepository;
// import com.song.project.repository.PostImageRepository;
// import com.song.project.repository.PostRepository;
// import com.song.project.repository.ReviewRepository;
// import com.song.project.repository.UserRepository;
// import com.song.project.service.PostViewCountService;
// import com.song.project.service.RecommendedPostService;
// import com.song.project.exception.NotFoundException;
// import com.song.project.exception.ForbiddenException;

// import lombok.RequiredArgsConstructor;

// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Sort;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.core.Authentication;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.validation.BindingResult;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// import jakarta.servlet.http.Cookie;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import jakarta.validation.Valid;

// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.UUID;
// import java.util.stream.Collectors;


// @Controller
// @RequiredArgsConstructor
// public class PostController {
//     private final PostRepository postRepository;
//     private final PostImageRepository postImageRepository;
//     private final LikeRepository likeRepository;
//     private final ReviewRepository reviewRepository;
//     private final UserRepository userRepository;
//     private final S3Service s3Service;
//     private final PostViewCountService postViewCountService;
//     private final RecommendedPostService recommendedPostService;

//     @GetMapping("/list")
//     String all_post(Model model,
//                     Authentication auth,
//                     @RequestParam(defaultValue = "1") int page,
//                     @RequestParam(required = false) String category,
//                     @RequestParam(required = false) String sort_by,
//                     @RequestParam(required = false) Integer start_price,
//                     @RequestParam(required = false) Integer end_price,
//                     @RequestParam(required = false) String status) {
//         List<String> categories = List.of(
//             "디지털기기", "생활가전", "가구/인테리어", "생활/주방",
//             "유아동", "유아도서", "여성의류", "여성잡화",
//             "남성패션/잡화", "뷰티/미용", "스포츠/레저",
//             "취미/게임/음반", "도서", "티켓/교환권",
//             "가공식품", "건강기능식품", "반려동물용품",
//             "식물", "기타 중고물품"
//         );
        
//         // 정렬 조건 설정
//         Sort sort = Sort.by(Sort.Direction.DESC, "created");
//         if ("hottest".equals(sort_by)) {
//             sort = Sort.by(Sort.Direction.DESC, "likeCnt");
//         }
        
//         PageRequest pageRequest = PageRequest.of(page - 1, 20, sort);
    
//         PostStatus postStatus = null;
//         String selectedStatus = null;
    
//         if (status != null && !status.isEmpty() && !"null".equals(status)) {
//             try {
//                 postStatus = PostStatus.valueOf(status.toUpperCase());
//                 selectedStatus = status; // 정상적인 상태 값
//             } catch (IllegalArgumentException e) {
//                 // 잘못된 상태 값은 무시
//             }
//         }

//         Page<Post> result = postRepository.findWithFilter(category, start_price, end_price, postStatus, pageRequest);

//         // Post -> PostEditDto 변환
//         // Page<PostEditDto> postDtos = result.map(PostEditDto::from);
//         Page<PostListDto> postDtos = result.map(PostListDto::from);

//         // likedPostIds를 if 블록 밖에서 선언 및 초기화
//         List<Long> likedPostIds = Collections.emptyList();

//         // 조회수 합산용 처리
//         List<Long> postIds = postDtos.stream()
//                 .map(PostListDto::getId)
//                 .collect(Collectors.toList());

//         // Redis 조회수 가져오기
//         Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);
//         System.out.println("조회수 조회 :" + redisViewCounts);

//         if (auth != null && auth.isAuthenticated()) {
//             CustomUser user = (CustomUser) auth.getPrincipal();

//             likedPostIds = likeRepository.findByUserId(user.id).stream()
//                     .map(like -> like.getPost().getId())
//                     .collect(Collectors.toList());

//             model.addAttribute("likedPostIds", likedPostIds); // 모델에 좋아요 누른 게시물 ID 목록 추가
//         }
//         model.addAttribute("likedPostIds", likedPostIds);
//         model.addAttribute("posts", postDtos);
//         model.addAttribute("currentPage", page);
//         model.addAttribute("totalPages", result.getTotalPages());
//         model.addAttribute("categories", categories);

//         model.addAttribute("selectedCategory", category);
//         model.addAttribute("selectedSort", sort_by);
//         model.addAttribute("selectedStartPrice", start_price);
//         model.addAttribute("selectedEndPrice", end_price);
//         model.addAttribute("selectedStatus", selectedStatus);
//         model.addAttribute("statuses", PostStatus.values());
//         model.addAttribute("viewCounts", redisViewCounts);

//         return "list.html";
//     }

//     @GetMapping("/search")
//     String search(Model model, Authentication auth,
//             @RequestParam String searchText,
//             @RequestParam(defaultValue = "1") int page) {
//         Page<Post> data = postRepository.fullTextSearchWithPaging(searchText,
//                 PageRequest.of(page - 1,20));

//         Page<PostListDto> postDtos = data.map(PostListDto::from);

//         List<Long> likedPostIds = Collections.emptyList();

//         if (auth != null && auth.isAuthenticated()) {
//             CustomUser user = (CustomUser) auth.getPrincipal();

//             likedPostIds = likeRepository.findByUserId(user.id).stream()
//                     .map(like -> like.getPost().getId())
//                     .collect(Collectors.toList());

//             model.addAttribute("likedPostIds", likedPostIds); // 모델에 좋아요 누른 게시물 ID 목록 추가
//         }
        
//         System.out.println("토탈페이지 :" + data.getTotalPages());

//         model.addAttribute("likedPostIds", likedPostIds);
// //        model.addAttribute("posts", data);
//         model.addAttribute("posts", postDtos);
//         model.addAttribute("currentPage", page);
//         model.addAttribute("totalPages", data.getTotalPages());
//         model.addAttribute("searchText", searchText);

//         return "search.html";
//     }

//     @GetMapping("/detail/{id}")
//     String show_post(Model model, @PathVariable Long id,  
//                         Authentication auth,
//                         HttpServletRequest request,
//                         HttpServletResponse response) {

//         Long loginUserId = null;
//         String viewToken = null;

//         if (auth != null && auth.isAuthenticated()) {
//             CustomUser user = (CustomUser) auth.getPrincipal();
//             loginUserId = user.id;
//             model.addAttribute("loginUserId", loginUserId);
//         }

//         model.addAttribute("loginUserId", loginUserId);

//         Optional<Post> data = postRepository.findById(id);

//         if (data.isEmpty()) {
//             return "redirect:/list";
//         }

//         model.addAttribute("data", data.get());
//         // Post 작성자 정보
//         model.addAttribute("postWriterId", data.get().getUser().getId());

//         if (loginUserId == null) {
//             // 쿠키에서 viewToken 확인
//             Cookie[] cookies = request.getCookies();
//             if (cookies != null) {
//                 for (Cookie cookie : cookies) {
//                     if ("viewToken".equals(cookie.getName())) {
//                         viewToken = cookie.getValue();
//                         break;
//                     }
//                 }
//             }

//             if (viewToken == null) {
//                 viewToken = UUID.randomUUID().toString();
//                 Cookie cookie = new Cookie("viewToken", viewToken);
//                 cookie.setPath("/");
//                 cookie.setMaxAge(60 * 60 * 24); // 1일 유효
//                 response.addCookie(cookie);
//             }
//         }

//         Long viewCount = postViewCountService.incrementAndGetViewCount(id, loginUserId, viewToken);
//         model.addAttribute("viewCount", viewCount);

//         // Redis 인기 게시글 점수 가중치 반영
//         recommendedPostService.addViewScore(id);

//         // ===========================
//         // 추천 상품 가져오기
//         // ===========================
//         // List<Long> recentPostIds = new ArrayList<>();
//         // if (loginUserId != null) {
//         //     recentPostIds = recentPostService.getRecentPosts(loginUserId);
//         // }

//         List<Long> recommendedPostIds = recommendedPostService.recommendPopularByCategory(id);
       
//         List<RecommendedPostDto> recommendedPosts = recommendedPostIds.stream()
//                 .map(pid -> postRepository.findById(pid))
//                 .filter(Optional::isPresent)
//                 .map(Optional::get)
//                 .map(RecommendedPostDto::from)
//                 .collect(Collectors.toList());
        
//         model.addAttribute("recommendedPosts", recommendedPosts);

//         return "detail.html";

//     } 

//     @GetMapping("/new-post")
//     @PreAuthorize("isAuthenticated()")
//     String add_post(Model model) {
//         List<String> categories = List.of(
//             "디지털기기", "생활가전", "가구/인테리어", "생활/주방",
//             "유아동", "유아도서", "여성의류", "여성잡화",
//             "남성패션/잡화", "뷰티/미용", "스포츠/레저",
//             "취미/게임/음반", "도서", "티켓/교환권",
//             "가공식품", "건강기능식품", "반려동물용품",
//             "식물", "기타 중고물품"
//         );
//         model.addAttribute("categories", categories);
//         return "add.html";
//     }

//     @PostMapping("/add")
//     @PreAuthorize("isAuthenticated()")
//     String addPost(@Valid @ModelAttribute PostCreateDto dto,
//                    BindingResult bindingResult,
//                    Authentication auth,
//                    RedirectAttributes redirectAttributes) {
        
//         if (bindingResult.hasErrors()) {
//             redirectAttributes.addFlashAttribute("errorMessage", 
//                 bindingResult.getFieldErrors().get(0).getDefaultMessage());
//             return "redirect:/new-post";
//         }
        
//         CustomUser customUser = (CustomUser) auth.getPrincipal();
//         User user = userRepository.findById(customUser.id).orElseThrow();

//         System.out.println("image 파일 :" + dto.getImage());
//         Post post = new Post();
//         post.setTitle(dto.getTitle());
//         post.setPrice(dto.getPrice());
//         post.setCategory(dto.getCategory());
//         post.setBody(dto.getBody());
//         post.setUser(user);
//         postRepository.save(post);

//         // 2. 이미지 URL 분리 (여러 개면 ,로 구분됨)
//         if (dto.getImage() != null && !dto.getImage().isEmpty()) {
//             String[] urls = dto.getImage().split(",");
//             for (String url : urls) {
//                 PostImage postImage = new PostImage();
//                 postImage.setImgUrl(url.trim());
//                 postImage.setPost(post);  // Post와 연결
//                 postImageRepository.save(postImage);
//             }
//         }

//         return "redirect:/list";
//     }

//     @GetMapping("/presigned-url")
//     @PreAuthorize("isAuthenticated()")
//     @ResponseBody
//     String getURL(@RequestParam String filename) {
//         // 확장자 추출
//         String extension = "";
//         int dotIndex = filename.lastIndexOf(".");
//         if (dotIndex != -1) {
//             extension = filename.substring(dotIndex);
//         }

//         // UUID로 unique 파일명 생성
//         String uniqueFileName = UUID.randomUUID().toString() + extension;

//         // S3에 저장될 경로 구성
//         String key = "project/" + uniqueFileName;

//         // Presigned URL 생성
//         String result = s3Service.createPresignedUrl(key);

//         return result;
//     }

//     @GetMapping("/edit/{id}")
//     @PreAuthorize("isAuthenticated()")
//     String edit(Model model, @PathVariable Long id, Authentication auth) {
//         Optional<Post> data = postRepository.findById(id);

//         if (data.isEmpty()) {
//             return "redirect:/list";
//         }
        
//         Post post = data.get();
        
//         // 작성자 검증
//         CustomUser user = (CustomUser) auth.getPrincipal();
//         if (!post.getUser().getId().equals(user.id)) {
//             throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
//         }

//         List<String> categories = List.of(
//             "디지털기기", "생활가전", "가구/인테리어", "생활/주방",
//             "유아동", "유아도서", "여성의류", "여성잡화",
//             "남성패션/잡화", "뷰티/미용", "스포츠/레저",
//             "취미/게임/음반", "도서", "티켓/교환권",
//             "가공식품", "건강기능식품", "반려동물용품",
//             "식물", "기타 중고물품"
//         );

//         // ⬇️ 이 부분 DTO해서 보내기
//         PostEditDto postEditDto = PostEditDto.from(post);
//         model.addAttribute("data", postEditDto);
//         model.addAttribute("categories", categories);
//         return "edit.html";
//     }

//     @PostMapping("/edit")
//     @PreAuthorize("isAuthenticated()")
//     String editPost(@Valid @ModelAttribute PostUpdateDto dto,
//                     BindingResult bindingResult,
//                     Authentication auth,
//                     RedirectAttributes redirectAttributes) {
        
//         if (bindingResult.hasErrors()) {
//             redirectAttributes.addFlashAttribute("errorMessage", 
//                 bindingResult.getFieldErrors().get(0).getDefaultMessage());
//             return "redirect:/edit/" + dto.getPostId();
//         }
        
//         Post post = postRepository.findById(dto.getPostId())
//             .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

//         // 작성자 검증
//         CustomUser user = (CustomUser) auth.getPrincipal();
//         if (!post.getUser().getId().equals(user.id)) {
//             throw new UnauthorizedException("본인이 작성한 게시글만 수정할 수 있습니다.");
//         }

//         post.setTitle(dto.getTitle());
//         post.setPrice(dto.getPrice());
//         post.setCategory(dto.getCategory());
//         post.setBody(dto.getBody());
//         postRepository.save(post);

//         // 3️⃣ 새로 업로드된 이미지 처리
//         if (dto.getImage() != null && !dto.getImage().isEmpty()) {
//             String[] urls = dto.getImage().split(",");
//             for (String url : urls) {
//                 PostImage postImage = new PostImage();
//                 postImage.setImgUrl(url.trim());
//                 postImage.setPost(post);  // Post와 연결
//                 postImageRepository.save(postImage);
//             }
//         }

//         return "redirect:/detail/" + dto.getPostId();
//     }

//     @DeleteMapping("/delete")
//     @PreAuthorize("isAuthenticated()")
//     ResponseEntity<String> delete(@RequestParam Long id, Authentication auth) {
//         Post post = postRepository.findById(id)
//             .orElseThrow(() -> new NotFoundException("게시물을 찾을 수 없습니다."));

//         // 작성자 검증
//         CustomUser user = (CustomUser) auth.getPrincipal();
//         if (!post.getUser().getId().equals(user.id)) {
//             throw new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다.");
//         }

//         for (PostImage img : post.getImages()) {
//             String key = s3Service.extractS3Key(img.getImgUrl());
//             s3Service.deleteFile(key);
//         }

//         postRepository.delete(post);

//         System.out.println("삭제완료");
//         return ResponseEntity.status(200).body("삭제완료");
//     }

//     @DeleteMapping("/delete-image")
//     @PreAuthorize("isAuthenticated")
//     ResponseEntity<String> deleteImages(@RequestParam Long imageId, Authentication auth) {
//         PostImage img = postImageRepository.findById(imageId)
//             .orElseThrow(() -> new NotFoundException("이미지를 찾을 수 없습니다."));

//         // 권한 수정 로직
//         // 해당 이미지가 속한 게시글의 작성자 검증
//         Post post = img.getPost();
//         if (post == null) {
//             throw new NotFoundException("게시글을 찾을 수 없습니다.");
//         }

//         CustomUser user = (CustomUser) auth.getPrincipal();
//         if (!post.getUser().getId().equals(user.id)) {
//             throw new ForbiddenException("본인이 작성한 게시글의 이미지만 삭제할 수 있습니다.");
//         }
//         // 여기까지

//         String key = s3Service.extractS3Key(img.getImgUrl());
//         s3Service.deleteFile(key);

//         postImageRepository.delete(img);

//         System.out.println("선택 이미지 삭제 완료: " + imageId);
//         return ResponseEntity.ok("삭제완료");
//     }

//     @PatchMapping("/post/{id}/status")
//     @PreAuthorize("isAuthenticated()")
//     @ResponseBody
//     ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
//                                                      @Valid @ModelAttribute PostStatusUpdateDto dto,
//                                                      BindingResult bindingResult,
//                                                      Authentication auth) {
//         Map<String, Object> response = new HashMap<>();
        
//         if (bindingResult.hasErrors()) {
//             response.put("success", false);
//             response.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
//             return ResponseEntity.status(400).body(response);
//         }
        
//         Post post = postRepository.findById(id)
//             .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

//         // 작성자 검증
//         CustomUser user = (CustomUser) auth.getPrincipal();
//         if (!post.getUser().getId().equals(user.id)) {
//             throw new ForbiddenException("본인이 작성한 게시글만 상태를 변경할 수 있습니다.");
//         }

//         // 상태 값 설정
//         PostStatus postStatus = PostStatus.valueOf(dto.getStatus().toUpperCase());
//         post.setStatus(postStatus);
//         postRepository.save(post);
        
//         response.put("success", true);
//         response.put("message", "상태가 변경되었습니다.");
//         response.put("status", postStatus.name());
//         response.put("statusDescription", postStatus.getDescription());
//         return ResponseEntity.ok(response);
//     }

//     @GetMapping("/seed")
//     @ResponseBody
//     String seedPosts() {
//         for (int i = 1; i <= 30; i++) {
//             Post seed = new Post();
//             seed.setTitle("테스트 제목" + i);
//             seed.setPrice((int)(Math.random() * 10000));
//             seed.setCategory("카테고리" + ((i % 5) + 1));
//             seed.setBody("테스트용 게시글 내용. 번호: " + i);

//             User user = userRepository.findById(1L).orElse(null);
//             seed.setUser(user);

//             postRepository.save(seed);
//         }
//         return "20개 게시글 생성";
//     }

//     @GetMapping("/profile/{username}")
//     String profileUser(Model model, @PathVariable String username, 
//                 @RequestParam(defaultValue = "1") int postPage,
//                 @RequestParam(defaultValue = "1") int reviewPage,
//                 @RequestParam(defaultValue = "posts") String tab,
//                                     Authentication auth) {
        
//         System.out.println("탭테스트: " + tab);

//         Long loginUserId = null;
//         if (auth != null) { // 로그인한 사용자만
//             CustomUser loggedUser = (CustomUser) auth.getPrincipal();
//             loginUserId = loggedUser.id;
//         }

//         User user = userRepository.findByUsername(username)
//             .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
//         UserProfileDto userDto = new UserProfileDto(user);
       
//         Page<Post> posts = postRepository.findByUser_Username(username, PageRequest.of(postPage - 1, 20));
//         // Post -> PostEditDto 변환
//         Page<PostListDto> postDtos = posts.map(PostListDto::from);

//         // 조회수 합산용 처리
//         List<Long> postIds = postDtos.stream()
//                 .map(PostListDto::getId)
//                 .collect(Collectors.toList());

//         // Redis 조회수 가져오기
//         Map<Long, Long> redisViewCounts = postViewCountService.getViewCountsForPosts(postIds);

//         Page<Review> reviews = reviewRepository.findByTargetUser_Id(userDto.getId(), PageRequest.of(reviewPage - 1,3));
//         // Page<Post> posts = Page.empty();
//         // Page<Review> reviews = Page.empty();

//         // if ("posts".equals(tab)) {
//         //     posts = postRepository.findByUser_Username(username, PageRequest.of(postPage - 1, 20));
//         // } else if ("reviews".equals(tab)) {
//         //     reviews = reviewRepository.findByTargetUser_Id(userDto.getId(), PageRequest.of(reviewPage - 1, 20));
//         // }

//         List<Long> likedPostIds = likeRepository.findByUserId(user.getId()).stream()
//         .map(like -> like.getPost().getId())
//         .collect(Collectors.toList());

//         model.addAttribute("user", userDto);
//         model.addAttribute("posts", postDtos);
//         model.addAttribute("reviews", reviews);
//         model.addAttribute("likedPostIds", likedPostIds);
//         model.addAttribute("postCurrentPage", postPage);
//         model.addAttribute("reviewCurrentPage", reviewPage);
//         model.addAttribute("postTotalPages", posts.getTotalPages());
//         model.addAttribute("reviewTotalPages", reviews.getTotalPages());
//         model.addAttribute("loginUserId", loginUserId);
//         model.addAttribute("viewCounts", redisViewCounts);

//         model.addAttribute("tab", tab);
        
//         return "profile.html";
//     }
// }