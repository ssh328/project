package com.song.project.entity;

import lombok.Getter;

@Getter
public enum SettlementStatus {
    PENDING("정산 대기"),
    SETTLED("정산 완료");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }
}
