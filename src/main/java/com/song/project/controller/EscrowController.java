package com.song.project.controller;

import com.song.project.entity.EscrowOrder;
import com.song.project.entity.Settlement;
import com.song.project.entity.Wallet;
import com.song.project.security.CustomUser;
import com.song.project.service.EscrowService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 에스크로 주문 관련 뷰 컨트롤러
 * 
 * 에스크로 주문 상세, 구매 내역, 판매 내역, 가상 정산 내역 페이지 제공
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/escrow")
public class EscrowController {

    private final EscrowService escrowService;

    /**
     * 에스크로 주문 상세 페이지 조회
     * 
     * @param orderId 조회할 주문 ID
     * @return 주문 상세 페이지 뷰 이름
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public String orderDetail(@PathVariable String orderId, Authentication auth, Model model) {
        Long userId = getUserId(auth);
        EscrowOrder order = escrowService.getOrder(orderId, userId);
        model.addAttribute("order", order);
        model.addAttribute("loginUserId", userId);
        return "pay/escrow-order.html";
    }

    /**
     * 구매 내역(구매자 관점) 페이지 조회
     * 
     * @return 구매 내역 페이지 뷰 이름
     */
    @GetMapping("/buy")
    @PreAuthorize("isAuthenticated()")
    public String buyHistory(Authentication auth, Model model) {
        Long userId = getUserId(auth);
        var orders = escrowService.getMyBuyingOrders(userId);
        model.addAttribute("orders", orders);
        model.addAttribute("loginUserId", userId);
        return "pay/buy-history.html";
    }

    /**
     * 판매 내역(판매자 관점) 페이지 조회
     * 
     * @return 판매 내역 페이지 뷰 이름
     */
    @GetMapping("/sell")
    @PreAuthorize("isAuthenticated()")
    public String sellHistory(Authentication auth, Model model) {
        Long userId = getUserId(auth);
        var orders = escrowService.getMySellingOrders(userId);
        model.addAttribute("orders", orders);
        return "pay/sell-history.html";
    }

    /**
     * 정산 내역(판매자 지갑/정산내역) 페이지 조회
     * 실제 송금이 아니라 구매확정 시 판매자에게 정산금이 적립되는 것을 시각화하기 위한 페이지
     * 
     * @return 정산 내역 페이지 뷰 이름
     */
    @GetMapping("/settlements")
    @PreAuthorize("isAuthenticated()")
    public String settlementHistory(Authentication auth, Model model) {
        Long userId = getUserId(auth);
        Wallet wallet = escrowService.getMyWallet(userId);
        List<Settlement> settlements = escrowService.getMySettlementHistory(userId);
        model.addAttribute("wallet", wallet);
        model.addAttribute("settlements", settlements);
        return "pay/settlement-history.html";
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
