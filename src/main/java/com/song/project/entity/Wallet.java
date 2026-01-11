package com.song.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 가상 정산 계정(지갑)
 * 실제 송금/PG 정산이 아니라, 구매확정 시 판매자에게 정산금이 적립되는 것을 시각화하기 위한 테이블
 */
@Entity
@Getter
@Setter
@ToString
@Table(
        name = "wallet",
        indexes = {
                @Index(name = "idx_wallet_user", columnList = "userId")
        }
)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 지갑 ID

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user; // 사용자

    @Column(nullable = false)
    private Long balance = 0L; // 잔액

    @CreationTimestamp
    private LocalDateTime createdAt; // 생성일시

    @UpdateTimestamp
    private LocalDateTime updatedAt; // 수정일시
}
