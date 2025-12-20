// 게시글 ID (전역 변수에서 가져옴)
const postId = window.postId;
// CSRF 토큰 및 헤더 이름 (meta 태그에서 추출)
const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');

// 최근 본 게시글 서버에 추가 (로그인한 경우에만 처리)
fetch(`/recent-posts/add/${postId}`, {
    method: 'POST',
    headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrfToken  // CSRF 토큰을 동적 헤더 이름으로 설정
    }
});

// 채팅 버튼 클릭 이벤트 리스너 등록
const chatButton = document.getElementById("chatButton");

if (chatButton) {
    // data 속성에서 게시글 작성자 ID 추출
    const postWriterId = chatButton.getAttribute("data-post-writer-id");
    chatButton.addEventListener("click", function () {
        // 채팅 페이지로 이동 (게시글 작성자 ID 전달)
        window.location.href = `/chat?postWriterId=${postWriterId}`;
    });
}

/**
 * 게시글 상태 변경
 * 사용자 확인 후 서버에 상태 변경 요청을 보내고 결과에 따라 페이지를 새로고침
 * @param {string} status - 변경할 상태 값
 */
function changeStatus(status) {

    // 사용자 확인
    if (!confirm('상태를 변경하시겠습니까?')) {
        return;
    }

    // 게시글 상태 변경 API 요청
    fetch(`/post/${postId}/status?status=${status}`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken  // CSRF 토큰을 동적 헤더 이름으로 설정
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            // 상태 변경 성공 시 페이지 새로고침하여 변경된 상태 반영
            location.reload();
        } else {
            alert(data.message || '상태 변경에 실패했습니다.');
        }
    })
    .catch(error => {
        console.error('에러:', error);
        alert('상태 변경 중 오류가 발생했습니다.');
    });
}