package com.song.project.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

/**
 * AWS S3 관련 기능을 제공하는 서비스
 * Presigned URL 생성, 파일 삭제, S3 Key 추출 기능 제공
 */
@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    /**
     * S3 파일 업로드를 위한 Presigned URL 생성
     * 클라이언트가 직접 S3에 업로드할 수 있는 임시 URL 반환 (3분 유효)
     * @param path S3에 저장될 파일 경로 (예: "project/uuid.jpg")
     * @return Presigned URL
     */
    public String createPresignedUrl(String path) {

        // S3 PutObject 요청 생성
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build();

        // Presigned URL 요청 생성 (3분 유효)
        var preSignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(3))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(preSignRequest).url().toString();
    }

    /**
     * S3에서 파일 삭제
     * @param key 삭제할 파일의 S3 Key (예: "project/uuid.jpg")
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * S3 이미지 URL에서 S3 Key 추출
     * 예: "https://bucket.s3.region.amazonaws.com/project/uuid.jpg" -> "project/uuid.jpg"
     * @param imageUrl S3 이미지 전체 URL
     * @return S3 Key (경로 앞의 "/" 제거)
     * @throws RuntimeException URL 파싱 실패 시
     */
    public String extractS3Key(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            // 경로 앞의 "/" 제거하여 S3 Key 형식으로 변환
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            throw new RuntimeException("S3 Key 추출 실패: " + imageUrl, e);
        }
    }
}
