const postId = window.postId;
const csrfToken = document.querySelector('meta[name="csrf-token"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="csrf-header"]').getAttribute('content');

// 최근 본 상품 서버로 전송 (로그인한 경우)
fetch(`/recent-posts/add/${postId}`, {
    method: 'POST',
    headers: {
        "Content-Type": "application/json",
        [csrfHeader]: csrfToken
    }
});

// ✅ 버튼 클릭 시 채팅 실행
const chatButton = document.getElementById("chatButton");

if (chatButton) {
    const postWriterId = chatButton.getAttribute("data-post-writer-id");
    chatButton.addEventListener("click", function () {
        window.location.href = `/chat?postWriterId=${postWriterId}`;
    });
}

// 상태 변경 함수
function changeStatus(status) {

    if (!confirm('상태를 변경하시겠습니까?')) {
        return;
    }

    fetch(`/post/${postId}/status?status=${status}`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            // 페이지 새로고침하여 변경된 상태 반영
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