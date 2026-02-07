package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Table(
        name = "escrow_order",
        indexes = {
                @Index(name = "idx_escrow_order_orderId", columnList = "orderId"),
                @Index(name = "idx_escrow_order_buyer", columnList = "buyerId"),
                @Index(name = "idx_escrow_order_seller", columnList = "sellerId")
        }
)
// 주문 자체에 대한 기록
// “이 거래가 지금 어디까지 진행됐는지(결제→배송→구매확정→정산완료)”를 추적하는 테이블
public class EscrowOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 주문 ID

    @Column(nullable = false, unique = true, length = 64)
    private String orderId; // Toss Payments orderId와 동일하게 사용 (포트폴리오용)

    @Column(nullable = false, length = 250)
    private String orderName; // 주문명

    @Column(nullable = false)
    private Integer amount; // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EscrowStatus status = EscrowStatus.CREATED; // 에스크로 상태

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderType orderType = OrderType.ESCROW; // 거래 유형 (기본값: 에스크로)

    // 결제 승인 후 Toss에서 내려주는 값들(최소만 저장)
    @Column(length = 200)
    private String paymentKey; // 결제 키

    @Column(length = 50)
    private String paymentMethod; // 결제 수단

    @CreationTimestamp
    private LocalDateTime createdAt; // 주문 생성일시

    private LocalDateTime paidAt; // 결제 완료일시
    private LocalDateTime deliveredAt; // 배송 완료일시
    private LocalDateTime purchaseConfirmedAt; // 구매 확정일시
    private LocalDateTime settledAt; // 정산 완료일시

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postId", nullable = false)
    private Post post; // 게시글

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyerId", nullable = false)
    private User buyer; // 구매자

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerId", nullable = false)
    private User seller; // 판매자
}
