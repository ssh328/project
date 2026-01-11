package com.song.project.controller;

import com.song.project.entity.EscrowOrder;
import com.song.project.security.CustomUser;
import com.song.project.service.EscrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 에스크로 주문 관련 REST API 컨트롤러
 * 
 * 에스크로 주문 조회, 배송 완료 처리, 구매 확정 등 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/escrow")
public class EscrowApiController {

    private final EscrowService escrowService;

    /**
     * 에스크로 주문 정보를 조회
     * 
     * @param orderId 조회할 주문 ID
     * @return 주문 정보를 담은 ResponseEntity
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> get(@PathVariable String orderId, Authentication auth) {
        Long userId = getUserId(auth);
        EscrowOrder order = escrowService.getOrder(orderId, userId);
        return ResponseEntity.ok(toResponse(order));
    }

    /**
     * 에스크로 주문의 배송 완료 상태로 변경
     * 
     * @param orderId 배송 완료 처리할 주문 ID
     * @return 업데이트된 주문 정보를 담은 ResponseEntity
     */
    @PostMapping("/orders/{orderId}/deliver")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deliver(@PathVariable String orderId, Authentication auth) {
        Long userId = getUserId(auth);
        EscrowOrder order = escrowService.markDelivered(orderId, userId);
        return ResponseEntity.ok(toResponse(order));
    }

    /**
     * 에스크로 주문의 구매 확정을 처리
     * 
     * @param orderId 구매 확정할 주문 ID
     * @return 업데이트된 주문 정보를 담은 ResponseEntity
     */
    @PostMapping("/orders/{orderId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirm(@PathVariable String orderId, Authentication auth) {
        Long userId = getUserId(auth);
        EscrowOrder order = escrowService.confirmPurchase(orderId, userId);
        return ResponseEntity.ok(toResponse(order));
    }

    /**
     * EscrowOrder 엔티티를 API 응답용 Map으로 변환
     * 
     * @param order 변환할 에스크로 주문 엔티티
     * @return 주문 정보를 담은 Map 객체
     */
    private Map<String, Object> toResponse(EscrowOrder order) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getOrderId());
        response.put("postId", order.getPost().getId());
        response.put("amount", order.getAmount());
        response.put("status", order.getStatus().name());
        response.put("statusDescription", order.getStatus().getDescription());
        response.put("buyerId", order.getBuyer().getId());
        response.put("sellerId", order.getSeller().getId());
        response.put("paidAt", order.getPaidAt()); // null 허용
        response.put("deliveredAt", order.getDeliveredAt()); // null 허용
        response.put("purchaseConfirmedAt", order.getPurchaseConfirmedAt()); // null 허용
        response.put("settledAt", order.getSettledAt()); // null 허용
        return response;
    }

    /**
     * 인증 정보에서 사용자 ID를 추출
     * 인증되지 않은 사용자는 null 반환
     * @param auth 인증 정보
     * @return 사용자 ID (인증된 경우), null (인증되지 않은 경우)
     */
    private Long getUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            CustomUser user = (CustomUser) auth.getPrincipal();
            return user.id;
        }
        return null;
    }
}
