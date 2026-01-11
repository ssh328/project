package com.song.project.entity;

import lombok.Getter;

@Getter
public enum EscrowStatus {
    CREATED("주문 생성"),
    PAYMENT_CONFIRMED("결제 승인(에스크로 보관)"),
    DELIVERY_MARKED("판매자 배송완료 처리"),
    PURCHASE_CONFIRMED("구매 확정"),
    SETTLED("가상 정산 완료"),
    FAILED("결제 실패");

    private final String description; // 상태 설명

    EscrowStatus(String description) {
        this.description = description;
    }
}
