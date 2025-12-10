package com.song.project.controller;

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

@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    String createPresignedUrl(String path) {

        var putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build();

        var preSignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(3))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(preSignRequest).url().toString();
    }

    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public String extractS3Key(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            throw new RuntimeException("S3 Key 추출 실패: " + imageUrl, e);
        }
    }
}
