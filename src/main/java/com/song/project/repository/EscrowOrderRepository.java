package com.song.project.repository;

import com.song.project.entity.EscrowOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EscrowOrderRepository extends JpaRepository<EscrowOrder, Long> {
    // 주문 ID로 에스크로 주문 조회
    @EntityGraph(attributePaths = {"post", "buyer", "seller"})
    Optional<EscrowOrder> findByOrderId(String orderId);

    // 구매자 ID로 에스크로 주문 목록 조회 (ID 내림차순 정렬)
    @EntityGraph(attributePaths = {"post", "buyer", "seller"})
    List<EscrowOrder> findByBuyer_IdOrderByIdDesc(Long buyerId);

    // 판매자 ID로 에스크로 주문 목록 조회 (ID 내림차순 정렬)
    @EntityGraph(attributePaths = {"post", "buyer", "seller"})
    List<EscrowOrder> findBySeller_IdOrderByIdDesc(Long sellerId);

    List<EscrowOrder> findByPost_Id(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE EscrowOrder e SET e.post = null WHERE e.post.id = :postId")
    void detachPostByPostId(@Param("postId") Long postId);
}
