package com.song.project.service.post;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.song.project.entity.Post;
import com.song.project.entity.PostImage;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.repository.PostImageRepository;
import com.song.project.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 게시글 이미지 및 S3 연동 관련 비즈니스 로직을 전담하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageService {

    private final PostImageRepository postImageRepository;
    private final S3Service s3Service;

    /**
     * 게시글 이미지 저장
     * @param post 게시글 엔티티
     * @param imageUrls 콤마로 구분된 이미지 URL 문자열
     */
    @Transactional
    public void saveImages(Post post, String imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            String[] urls = imageUrls.split(",");
            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (!trimmedUrl.isEmpty()) {
                    try {
                        PostImage postImage = new PostImage();
                        postImage.setImgUrl(trimmedUrl);
                        postImage.setPost(post);
                        postImageRepository.save(postImage);
                    } catch (Exception e) {
                        log.error("이미지 저장 실패: postId={}, url={}", post.getId(), trimmedUrl, e);
                        throw new BadRequestException("이미지 저장에 실패했습니다.");
                    }
                }
            }
            log.debug("게시글 이미지 저장 완료: postId={}", post.getId());
        }
    }

    /**
     * S3에서 파일 삭제
     * @param imgUrl S3 파일 URL
     */
    public void deleteFileFromS3(String imgUrl) {
        String key = s3Service.extractS3Key(imgUrl);
        s3Service.deleteFile(key);
    }

    /**
     * 게시글 이미지 단건 삭제
     * @param imageId 삭제할 이미지 ID
     * @param userId 현재 로그인한 사용자 ID
     */
    @Transactional
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

        deleteFileFromS3(img.getImgUrl());
        postImageRepository.delete(img);
    }
}
