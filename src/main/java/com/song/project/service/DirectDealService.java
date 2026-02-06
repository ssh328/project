package com.song.project.service;

import com.song.project.entity.EscrowOrder;
import com.song.project.entity.EscrowStatus;
import com.song.project.entity.OrderType;
import com.song.project.entity.Post;
import com.song.project.entity.PostChatCandidate;
import com.song.project.entity.PostStatus;
import com.song.project.entity.User;
import com.song.project.exception.BadRequestException;
import com.song.project.exception.ForbiddenException;
import com.song.project.exception.NotFoundException;
import com.song.project.repository.EscrowOrderRepository;
import com.song.project.repository.PostChatCandidateRepository;
import com.song.project.repository.PostRepository;
import com.song.project.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 직거래 판매완료(구매자 확정) 및 채팅 후보자 기록 관련 서비스
 */
@Service
@RequiredArgsConstructor
public class DirectDealService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostChatCandidateRepository postChatCandidateRepository;
    private final EscrowOrderRepository escrowOrderRepository;

    /**
     * 사용자가 특정 게시글의 판매자와 채팅을 "시작"한 사실을 후보로 기록한다.
     * - TalkJS를 쓰므로 메시지 저장 대신 "채팅 진입"을 서버에 남긴다.
     */
    @Transactional
    public void recordChatCandidate(Long postId, Long loginUserId, Long postWriterId) {
        if (postId == null || loginUserId == null || postWriterId == null) {
            return;
        }

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (post.getUser() == null) {
            return;
        }

        // URL 파라미터로 postWriterId를 위조하는 것을 방지: post.user.id와 일치해야 함
        if (!post.getUser().getId().equals(postWriterId)) {
            throw new BadRequestException("잘못된 채팅 요청입니다.");
        }

        // 본인 게시글에는 후보로 기록하지 않음
        if (post.getUser().getId().equals(loginUserId)) {
            return;
        }

        if (postChatCandidateRepository.existsByPost_IdAndUser_Id(postId, loginUserId)) {
            return;
        }

        User userRef = new User();
        userRef.setId(loginUserId);

        PostChatCandidate candidate = new PostChatCandidate();
        candidate.setPost(post);
        candidate.setUser(userRef);
        postChatCandidateRepository.save(candidate);
    }

    /**
     * 판매자(작성자)에게만 해당 게시글의 채팅 후보자 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<UserSummary> getChatCandidates(Long postId, Long sellerId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (post.getUser() == null || !post.getUser().getId().equals(sellerId)) {
            throw new ForbiddenException("작성자만 구매자 후보를 조회할 수 있습니다.");
        }

        return postChatCandidateRepository.findByPost_IdOrderByCreatedAtDesc(postId).stream()
            .map(c -> new UserSummary(c.getUser()))
            .collect(Collectors.toList());
    }

    /**
     * 직거래 판매완료 확정: 구매자를 선택해 Post에 저장하고 status를 SOLD로 변경한다.
     */
    @Transactional
    public void completeDirectDeal(Long postId, Long sellerId, Long buyerId) {
        if (buyerId == null) {
            throw new BadRequestException("구매자를 선택해 주세요.");
        }

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));

        if (post.getUser() == null || !post.getUser().getId().equals(sellerId)) {
            throw new ForbiddenException("작성자만 판매완료 처리를 할 수 있습니다.");
        }

        if (sellerId.equals(buyerId)) {
            throw new BadRequestException("본인을 구매자로 선택할 수 없습니다.");
        }

        // 후보자(채팅 진입)로 기록된 유저만 선택 가능
        if (!postChatCandidateRepository.existsByPost_IdAndUser_Id(postId, buyerId)) {
            throw new BadRequestException("해당 사용자는 이 게시글에 대해 채팅한 구매자 후보가 아닙니다.");
        }

        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new NotFoundException("구매자를 찾을 수 없습니다."));

        post.setStatus(PostStatus.SOLD);
        postRepository.save(post);

        // 직거래 완료 시 통합 주문(Order) 테이블에도 기록 생성 (OrderType.DIRECT)
        // -> 구매내역/판매내역에서 한 번에 조회하기 위함
        EscrowOrder order = new EscrowOrder();
        order.setOrderType(OrderType.DIRECT);
        order.setOrderId("DIRECT-" + UUID.randomUUID().toString()); // 직거래용 고유 ID
        order.setOrderName(post.getTitle());
        order.setAmount(post.getPrice());
        order.setStatus(EscrowStatus.SETTLED); // 직거래는 즉시 완료/정산됨 간주
        order.setPost(post);
        order.setBuyer(buyer);
        order.setSeller(post.getUser());
        
        // 직거래이므로 결제/배송/확정/정산 일시는 현재 시간으로 일괄 처리
        LocalDateTime now = LocalDateTime.now();
        order.setPaidAt(now);
        order.setDeliveredAt(now);
        order.setPurchaseConfirmedAt(now);
        order.setSettledAt(now);

        escrowOrderRepository.save(order);
    }

    @Getter
    public static class UserSummary {
        private final Long id;
        private final String username;
        private final String dp;

        public UserSummary(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.dp = user.getDp();
        }
    }
}

