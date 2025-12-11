package com.song.project.entity;

public enum PostStatus {
    ON_SALE("판매중"),
    RESERVED("예약중"),
    SOLD("판매완료");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
