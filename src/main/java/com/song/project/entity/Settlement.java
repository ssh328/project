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
        name = "settlement",
        indexes = {
                @Index(name = "idx_settlement_seller", columnList = "sellerId")
        }
)
// 판매자에게 얼마나 정산됐는지에 대한 기록
// “이 주문 기준으로 판매자 지갑에 얼마가 언제 정산되었는지”를 표현하는 테이블
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 정산 ID

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escrowOrderId", nullable = false, unique = true)
    private EscrowOrder order; // 에스크로 주문

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerId", nullable = false)
    private User seller; // 판매자

    @Column(nullable = false)
    private Long grossAmount; // 총 금액

    @Column(nullable = false)
    private Long feeAmount; // 수수료

    @Column(nullable = false)
    private Long netAmount; // 정산 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.PENDING; // 정산 상태

    @CreationTimestamp
    private LocalDateTime createdAt; // 생성일시

    private LocalDateTime settledAt; // 정산 완료일시
}
