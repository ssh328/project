package com.song.project.repository;

import com.song.project.entity.Settlement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    // 주문 ID로 정산 내역 조회
    Optional<Settlement> findByOrder_OrderId(String orderId);

    // 판매자 ID로 정산 내역 목록 조회 (ID 내림차순 정렬)
    @EntityGraph(attributePaths = {"order", "order.post"})
    List<Settlement> findBySeller_IdOrderByIdDesc(Long sellerId);
}
