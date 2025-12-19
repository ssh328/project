package com.song.project.entity;

public enum PostStatus {
    ON_SALE("판매중"),
    RESERVED("예약중"),
    SOLD("판매완료");

    private final String description;

    // 게시물 상태 설명
    PostStatus(String description) {
        this.description = description;
    }

    // 게시물 상태 설명 반환
    public String getDescription() {
        return description;
    }
}
