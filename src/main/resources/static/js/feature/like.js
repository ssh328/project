// CSRF 토큰 및 헤더 이름 (meta 태그에서 추출)
const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');

/**
 * 모든 좋아요 버튼에 클릭 이벤트 리스너 등록
 * 게시글 좋아요 토글 기능을 처리
 */
document.querySelectorAll(".increase-cnt-btn").forEach(heart_btn => {
    /**
     * 좋아요 버튼 클릭 이벤트 핸들러
     * 서버에 좋아요 토글 요청을 보내고 UI를 업데이트
     * @this {HTMLElement} 클릭된 좋아요 버튼 요소
     */
    heart_btn.addEventListener('click', function() {
        // 버튼의 data-post-id 속성에서 게시글 ID 추출
        const postId = Number(this.dataset.postId);
        // 하트 아이콘 요소 선택
        const heartIcon = this.querySelector('i');

        // 좋아요 토글 API 요청
        fetch(`/like/${postId}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                [csrfHeader]: csrfToken  // CSRF 토큰을 동적 헤더 이름으로 설정
            }
        })
        .then(res => res.json())
        .then(data => {
            // 로그인하지 않은 경우 처리
            if (!data.loggedIn) {
                console.log("에러 발생")
                alert(data.message);
                window.location.href = "/login";
                return;
            }

            // 좋아요 상태에 따라 아이콘 스타일 변경
            if (heartIcon.classList.contains('fa-solid')) {
                // 좋아요 취소: 채워진 하트 → 빈 하트
                heartIcon.classList.remove('fa-solid', 'fa-bounce');
                heartIcon.classList.add('fa-regular');
                heartIcon.style.color = '#000000';
                heartIcon.style.removeProperty('--fa-animation-iteration-count');
            } else {
                // 좋아요 추가: 빈 하트 → 채워진 하트 (애니메이션 포함)
                heartIcon.classList.remove('fa-regular');
                heartIcon.classList.add('fa-solid', 'fa-bounce');
                heartIcon.style.color = '#ff0000';
                heartIcon.style.setProperty('--fa-animation-iteration-count', '1');
            }

            // 좋아요 수 업데이트
            this.querySelector(".like-count").textContent = data.likeCount;
        })
        .catch(err => console.error("좋아요 요청 실패:", err));
    });
});