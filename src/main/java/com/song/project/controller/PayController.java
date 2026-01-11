package com.song.project.controller;

import com.song.project.entity.EscrowOrder;
import com.song.project.entity.Post;
import com.song.project.entity.PostStatus;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.security.CustomUser;
import com.song.project.service.EscrowService;
import com.song.project.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 결제 관련 뷰 컨트롤러
 * 
 * 결제 위젯, 결제 승인, 결제 실패 페이지 제공
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/pay")
public class PayController {

    private final EscrowService escrowService;
    private final PostService postService;

    @Value("${toss.client-key}")
    private String tossClientKey;

    /**
     * 결제 위젯을 렌더링할 체크아웃 페이지 조회
     * 
     * @param postId 결제할 게시글 ID
     * @return 결제 위젯 페이지 뷰 이름
     */
    @GetMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public String checkout(@RequestParam Long postId, Authentication auth, Model model) {
        Long userId = getUserId(auth);
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }

        Post post = postService.getPostOrThrow(postId);
        if (post.getStatus() != PostStatus.ON_SALE) {
            throw new BadRequestException("판매중인 상품만 결제할 수 있습니다.");
        }
        if (post.getUser() == null) {
            throw new NotFoundException("판매자 정보를 찾을 수 없습니다.");
        }
        if (post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인 상품은 결제할 수 없습니다.");
        }

        model.addAttribute("tossClientKey", tossClientKey);
        model.addAttribute("postId", post.getId());
        model.addAttribute("postTitle", post.getTitle());
        model.addAttribute("amount", post.getPrice());
        model.addAttribute("customerKey", "user-" + userId); // 포트폴리오용 고정 키
        model.addAttribute("customerName", auth != null ? auth.getName() : "");

        return "pay/checkout.html";
    }

    /**
     * Toss 결제 성공 후 결제 승인 처리 및 주문 생성
     * 
     * @param paymentKey Toss 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @param postId 게시글 ID
     * @return 주문 상세 페이지로 리다이렉트
     */
    @GetMapping("/success")
    @PreAuthorize("isAuthenticated()")
    public String success(@RequestParam String paymentKey,
                          @RequestParam String orderId,
                          @RequestParam Integer amount,
                          @RequestParam Long postId,
                          Authentication auth,
                          RedirectAttributes redirectAttributes) {
        Long userId = getUserId(auth);
        // 결제 완료 시점에 주문 생성 + 결제 승인을 함께 처리
        EscrowOrder order = escrowService.createAndConfirmPayment(postId, orderId, paymentKey, amount, userId);
        redirectAttributes.addFlashAttribute("successMessage", "결제가 승인되었습니다. (에스크로 보관 중)");
        return "redirect:/escrow/orders/" + order.getOrderId();
    }

    /**
     * 결제 실패 페이지 조회
     * 
     * @param code 결제 실패 코드 (선택)
     * @param message 결제 실패 메시지 (선택)
     * @return 결제 실패 페이지 뷰 이름
     */
    @GetMapping("/fail")
    public String fail(@RequestParam(required = false) String code,
                       @RequestParam(required = false) String message,
                       Model model) {
        model.addAttribute("code", code);
        model.addAttribute("message", message);
        return "pay/fail.html";
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
