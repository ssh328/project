package com.song.project.service.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.dto.PostCreateDto;
import com.song.project.dto.PostStatusUpdateDto;
import com.song.project.dto.PostUpdateDto;
import com.song.project.entity.EscrowOrder;
import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.entity.PostStatus;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.exception.UnauthorizedException;
import com.song.project.repository.EscrowOrderRepository;
import com.song.project.repository.LikeRepository;
import com.song.project.repository.PostChatCandidateRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 관련 상태 변경(Create, Update, Delete)을 전담하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommandService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EscrowOrderRepository escrowOrderRepository;
    private final PostChatCandidateRepository postChatCandidateRepository;
    private final LikeRepository likeRepository;
    private final PostImageService postImageService;

    /**
     * 게시글 생성
     */
    @Transactional
    public Post createPost(Long userId, PostCreateDto dto) {
        User user = userRepository.findById(userId)
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

        // 이미지 저장 위임
        postImageService.saveImages(saved, dto.getImage());

        return saved;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public Post updatePost(PostUpdateDto dto, Long userId) {
        Post post = postRepository.findActiveById(dto.getPostId())
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

        // 새로 업로드된 이미지 처리
        postImageService.saveImages(updated, dto.getImage());

        return updated;
    }

    /**
     * 게시글 삭제 (Soft Delete) 공통 로직
     */
    @Transactional
    public void deletePostInternal(Post post) {
        if (post.isDeleted()) {
            throw new BadRequestException("이미 삭제된 게시글입니다.");
        }

        try {
            post.setDeleted(true);
            post.setDeletedAt(LocalDateTime.now());

            postChatCandidateRepository.deleteAllByPostId(post.getId());
            likeRepository.deleteAllByPostId(post.getId());
            postRepository.save(post);

            log.info("게시글 soft delete 성공: postId={}, title={}",
                post.getId(), post.getTitle());
        } catch (Exception e) {
            log.error("게시물 soft delete 실패: postId={}, title={}", post.getId(), post.getTitle(), e);
            throw new BadRequestException("게시물 삭제에 실패했습니다.");
        }
    }

    /**
     * 게시글 영구 삭제 (Hard Delete) 공통 로직
     */
    @Transactional
    public void hardDeletePostInternal(Post post) {
        if (!post.isDeleted()) {
            throw new BadRequestException("영구 삭제는 먼저 soft delete된 게시글에 대해서만 가능합니다.");
        }

        try {
            List<PostImage> images = new ArrayList<>(post.getImages());
            List<EscrowOrder> relatedOrders = escrowOrderRepository.findByPost_Id(post.getId());

            for (EscrowOrder order : relatedOrders) {
                order.fillMissingSnapshotFromPost(post);
            }
            if (!relatedOrders.isEmpty()) {
                escrowOrderRepository.saveAll(relatedOrders);
                escrowOrderRepository.detachPostByPostId(post.getId());
            }

            postChatCandidateRepository.deleteAllByPostId(post.getId());
            likeRepository.deleteAllByPostId(post.getId());

            for (PostImage img : images) {
                postImageService.deleteFileFromS3(img.getImgUrl());
            }

            postRepository.delete(post);
            
            log.info("게시글 hard delete 성공: postId={}, title={}", 
                post.getId(), post.getTitle());
        } catch (Exception e) {
            log.error("게시물 hard delete 실패: postId={}, title={}", post.getId(), post.getTitle(), e);
            throw new BadRequestException("게시물 영구 삭제에 실패했습니다.");
        }
    }

    @Transactional
    public void restorePostInternal(Post post) {
        if (!post.isDeleted()) {
            throw new BadRequestException("이미 활성 상태인 게시글입니다.");
        }

        try {
            post.setDeleted(false);
            post.setDeletedAt(null);
            postRepository.save(post);

            log.info("게시글 복구 성공: postId={}, title={}",
                post.getId(), post.getTitle());
        } catch (Exception e) {
            log.error("게시글 복구 실패: postId={}, title={}", post.getId(), post.getTitle(), e);
            throw new BadRequestException("게시글 복구에 실패했습니다.");
        }
    }

    /**
     * 게시글 삭제 (사용자 요청)
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException("게시물을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인이 작성한 게시글만 삭제할 수 있습니다.");
        }

        deletePostInternal(post);
    }

    /**
     * 게시글 상태 변경
     */
    @Transactional
    public PostStatus updateStatus(Long postId, PostStatusUpdateDto dto, Long userId) {
        Post post = postRepository.findActiveById(postId)
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
}
