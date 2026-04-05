package com.song.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.song.project.entity.*;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.repository.EscrowOrderRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.SettlementRepository;
import com.song.project.repository.WalletRepository;
import com.song.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 에스크로 프로세스 서비스
 * 
 * 결제 승인, 배송 완료, 구매 확정, 정산 처리 제공
 */
@Service
@RequiredArgsConstructor
public class EscrowService {

    // 포트폴리오용 수수료(예: 3%) – 실제 PG 정산 수수료와 무관
    private static final double FEE_RATE = 0.03;

    private final EscrowOrderRepository escrowOrderRepository;
    private final SettlementRepository settlementRepository;
    private final WalletRepository walletRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TossPaymentsClient tossPaymentsClient;

    /**
     * 결제 완료 시점에 주문 생성 및 결제 승인 처리
     * 
     * @param postId 게시글 ID
     * @param orderId 주문 ID
     * @param paymentKey 결제 키
     * @param amount 결제 금액
     * @param buyerId 구매자 ID
     * @return 생성된 에스크로 주문
     */
    @Transactional
    public EscrowOrder createAndConfirmPayment(Long postId, String orderId, String paymentKey, Integer amount, Long buyerId) {
        // 주문이 이미 존재하는지 확인 (중복 요청 방지)
        EscrowOrder order = escrowOrderRepository.findByOrderId(orderId).orElse(null);
        
        if (order != null) {

            if (!order.getBuyer().getId().equals(buyerId)) {
                throw new ForbiddenException("구매자만 결제를 승인할 수 있습니다.");
            }
            if (!order.getAmount().equals(amount)) {
                throw new BadRequestException("결제 금액이 주문 금액과 일치하지 않습니다.");
            }
            // 이미 승인된 경우 중복 처리 방지
            if (order.getStatus() == EscrowStatus.PAYMENT_CONFIRMED ||
                    order.getStatus() == EscrowStatus.DELIVERY_MARKED ||
                    order.getStatus() == EscrowStatus.PURCHASE_CONFIRMED ||
                    order.getStatus() == EscrowStatus.SETTLED) {
                return order;
            }
        } else {
            // 주문이 없으면 생성
            Post post = postRepository.findActiveById(postId)
                    .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

            if (post.getStatus() != PostStatus.ON_SALE) {
                throw new BadRequestException("판매중인 상품만 결제할 수 있습니다.");
            }

            User buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            User seller = post.getUser();
            if (seller == null) {
                throw new BadRequestException("판매자 정보를 찾을 수 없습니다.");
            }
            if (seller.getId().equals(buyerId)) {
                throw new ForbiddenException("본인 상품은 결제할 수 없습니다.");
            }

            order = new EscrowOrder();
            order.setOrderId(orderId); // 클라이언트에서 생성한 orderId 사용
            order.setStatus(EscrowStatus.CREATED);
            order.setPost(post);
            order.setBuyer(buyer);
            order.setSeller(seller);
            order.capturePostSnapshot(post);
            order.setAmount(amount);
            escrowOrderRepository.save(order);
        }

        // Toss Payments 결제 승인
        JsonNode confirmResponse = tossPaymentsClient.confirm(paymentKey, orderId, amount);
        String method = confirmResponse.path("method").asText(null);

        order.setPaymentKey(paymentKey);
        order.setPaymentMethod(method);
        order.setStatus(EscrowStatus.PAYMENT_CONFIRMED);
        order.setPaidAt(LocalDateTime.now());

        // 원본 게시글이 남아 있으면 에스크로 진행 중 예약 상태로 잠금
        Post post = order.getPost();
        if (post != null) {
            post.setStatus(PostStatus.RESERVED);
            postRepository.save(post);
        }

        return escrowOrderRepository.save(order);
    }

    /**
     * 에스크로 주문의 배송 완료 상태로 변경
     * 
     * @param orderId 주문 ID
     * @param loginUserId 로그인 사용자 ID
     * @return 업데이트된 에스크로 주문
     */
    @Transactional
    public EscrowOrder markDelivered(String orderId, Long loginUserId) {
        EscrowOrder order = escrowOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        if (!order.getSeller().getId().equals(loginUserId)) {
            throw new ForbiddenException("판매자만 배송완료 처리를 할 수 있습니다.");
        }

        if (order.getStatus() != EscrowStatus.PAYMENT_CONFIRMED) {
            throw new BadRequestException("결제 승인 이후에만 배송완료 처리를 할 수 있습니다.");
        }

        order.setStatus(EscrowStatus.DELIVERY_MARKED);
        order.setDeliveredAt(LocalDateTime.now());
        return escrowOrderRepository.save(order);
    }

    /**
     * 에스크로 주문의 구매 확정 처리 및 정산
     * 
     * @param orderId 주문 ID
     * @param loginUserId 로그인 사용자 ID
     * @return 업데이트된 에스크로 주문
     */
    @Transactional
    public EscrowOrder confirmPurchase(String orderId, Long loginUserId) {
        EscrowOrder order = escrowOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        if (!order.getBuyer().getId().equals(loginUserId)) {
            throw new ForbiddenException("구매자만 구매확정 할 수 있습니다.");
        }

        if (order.getStatus() != EscrowStatus.DELIVERY_MARKED) {
            throw new BadRequestException("판매자가 배송완료 처리한 후에만 구매확정 할 수 있습니다.");
        }

        order.setStatus(EscrowStatus.PURCHASE_CONFIRMED);
        order.setPurchaseConfirmedAt(LocalDateTime.now());
        escrowOrderRepository.save(order);

        // 구매확정 -> 에스크로 해제 -> 가상 정산
        Settlement settlement = settlementRepository.findByOrder_OrderId(orderId)
                .orElseGet(() -> createSettlement(order));

        if (settlement.getStatus() != SettlementStatus.SETTLED) {
            settleToWallet(settlement);
        }

        // 원본 게시글이 남아 있으면 판매완료 상태도 함께 반영
        Post post = order.getPost();
        if (post != null) {
            post.setStatus(PostStatus.SOLD);
            postRepository.save(post);
        }

        order.setStatus(EscrowStatus.SETTLED);
        order.setSettledAt(LocalDateTime.now());
        return escrowOrderRepository.save(order);
    }

    /**
     * 에스크로 주문 조회
     * 
     * @param orderId 주문 ID
     * @param loginUserId 로그인 사용자 ID
     * @return 에스크로 주문
     */
    @Transactional(readOnly = true)
    public EscrowOrder getOrder(String orderId, Long loginUserId) {
        EscrowOrder order = escrowOrderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        // 구매자/판매자만 접근
        boolean isBuyer = order.getBuyer().getId().equals(loginUserId);
        boolean isSeller = order.getSeller().getId().equals(loginUserId);
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("해당 주문에 접근할 권한이 없습니다.");
        }
        return order;
    }

    /**
     * 내 구매 주문 목록 조회
     * 
     * @param loginUserId 로그인 사용자 ID
     * @return 구매 주문 목록
     */
    @Transactional(readOnly = true)
    public List<EscrowOrder> getMyBuyingOrders(Long loginUserId) {
        return escrowOrderRepository.findByBuyer_IdOrderByIdDesc(loginUserId);
    }

    /**
     * 내 판매 주문 목록 조회
     * 
     * @param loginUserId 로그인 사용자 ID
     * @return 판매 주문 목록
     */
    @Transactional(readOnly = true)
    public List<EscrowOrder> getMySellingOrders(Long loginUserId) {
        return escrowOrderRepository.findBySeller_IdOrderByIdDesc(loginUserId);
    }

    /**
     * 내 정산 내역 조회
     * 
     * @param loginUserId 로그인 사용자 ID
     * @return 정산 내역 목록
     */
    @Transactional(readOnly = true)
    public List<Settlement> getMySettlementHistory(Long loginUserId) {
        return settlementRepository.findBySeller_IdOrderByIdDesc(loginUserId);
    }

    /**
     * 내 지갑 조회
     * 
     * @param loginUserId 로그인 사용자 ID
     * @return 지갑 정보 (없으면 null)
     */
    @Transactional(readOnly = true)
    public Wallet getMyWallet(Long loginUserId) {
        return walletRepository.findByUser_Id(loginUserId).orElse(null);
    }

    // ===========================
    // 유틸리티
    // ===========================

    /**
     * 정산 내역 생성
     * 
     * @param order 에스크로 주문
     * @return 생성된 정산 내역
     */
    private Settlement createSettlement(EscrowOrder order) {
        long gross = order.getAmount().longValue();
        long fee = Math.round(gross * FEE_RATE);
        long net = gross - fee;

        Settlement s = new Settlement();
        s.setOrder(order);
        s.setSeller(order.getSeller());
        s.setGrossAmount(gross);
        s.setFeeAmount(fee);
        s.setNetAmount(net);
        s.setStatus(SettlementStatus.PENDING);
        return settlementRepository.save(s);
    }

    /**
     * 정산 금액을 판매자 지갑에 적립
     * 
     * @param settlement 정산 내역
     */
    private void settleToWallet(Settlement settlement) {
        User seller = settlement.getSeller();
        Wallet wallet = walletRepository.findByUser_Id(seller.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(seller);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });

        wallet.setBalance(wallet.getBalance() + settlement.getNetAmount());
        walletRepository.save(wallet);

        settlement.setStatus(SettlementStatus.SETTLED);
        settlement.setSettledAt(LocalDateTime.now());
        settlementRepository.save(settlement);
    }
}
