package com.song.project.repository;

import com.song.project.entity.EscrowOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EscrowOrderRepository extends JpaRepository<EscrowOrder, Long> {
    // 주문 ID로 에스크로 주문 조회
    @EntityGraph(attributePaths = {"post.images", "buyer", "seller"})
    Optional<EscrowOrder> findByOrderId(String orderId);

    // 구매자 ID로 에스크로 주문 목록 조회 (ID 내림차순 정렬)
    @EntityGraph(attributePaths = {"post.images", "buyer", "seller"})
    List<EscrowOrder> findByBuyer_IdOrderByIdDesc(Long buyerId);

    // 판매자 ID로 에스크로 주문 목록 조회 (ID 내림차순 정렬)
    @EntityGraph(attributePaths = {"post.images", "buyer", "seller"})
    List<EscrowOrder> findBySeller_IdOrderByIdDesc(Long sellerId);
}
