package com.song.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderType {
    ESCROW("안전결제"),
    DIRECT("직거래");

    private final String description;
}
