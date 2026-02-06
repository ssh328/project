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
        // postId도 함께 전달해 "채팅 시작"을 후보로 기록할 수 있게 함
        window.location.href = `/chat?postWriterId=${postWriterId}&postId=${postId}`;
    });
}

/**
 * 게시글 상태 변경
 * 사용자 확인 후 서버에 상태 변경 요청을 보내고 결과에 따라 페이지를 새로고침
 * @param {string} status - 변경할 상태 값
 */
function changeStatus(status) {

    // 판매완료(SOLD)는 구매자 선택(직거래 확정) 모달을 통해 처리
    if (status === 'SOLD') {
        openDirectDealModal();
        return;
    }

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

/**
 * 직거래 판매완료 처리: 채팅 후보자 목록을 불러와 구매자를 선택한 뒤 확정 API 호출
 */
function openDirectDealModal() {
    const modalEl = document.getElementById('directDealModal');
    if (!modalEl) {
        alert('구매자 선택 UI를 찾을 수 없습니다.');
        return;
    }

    const listEl = document.getElementById('directDealCandidateList');
    const emptyEl = document.getElementById('directDealEmpty');
    const confirmBtn = document.getElementById('directDealConfirmBtn');

    if (listEl) listEl.innerHTML = '';
    if (emptyEl) emptyEl.classList.add('d-none');
    if (confirmBtn) confirmBtn.disabled = true;

    // 후보 목록 조회
    fetch(`/post/${postId}/chat-candidates`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        }
    })
    .then(response => response.json())
    .then(data => {
        if (!data.success) {
            alert(data.message || '구매자 후보 조회에 실패했습니다.');
            return;
        }

        const candidates = data.candidates || [];
        if (candidates.length === 0) {
            if (emptyEl) emptyEl.classList.remove('d-none');
            return;
        }

        candidates.forEach(c => {
            const item = document.createElement('label');
            item.className = 'list-group-item d-flex align-items-center gap-2';
            item.innerHTML = `
                <input class="form-check-input me-2" type="radio" name="directDealBuyer" value="${c.id}">
                <img src="${c.dp || 'https://placehold.co/40'}" class="rounded-circle" width="32" height="32" alt="dp" style="object-fit: cover;">
                <span class="fw-semibold">${c.username || '사용자'}</span>
            `;
            item.addEventListener('click', () => {
                const radio = item.querySelector('input[type="radio"]');
                if (radio) radio.checked = true;
                if (confirmBtn) confirmBtn.disabled = false;
            });
            listEl && listEl.appendChild(item);
        });
    })
    .catch(err => {
        console.error(err);
        alert('구매자 후보 조회 중 오류가 발생했습니다.');
    });

    // 모달 오픈
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    modal.show();

    // 확정 버튼 핸들러(중복 등록 방지 위해 onclick 사용)
    if (confirmBtn) {
        confirmBtn.onclick = function () {
            const checked = document.querySelector('input[name="directDealBuyer"]:checked');
            if (!checked) {
                alert('구매자를 선택해 주세요.');
                return;
            }

            const buyerId = checked.value;
            fetch(`/post/${postId}/direct-deal/complete?buyerId=${buyerId}`, {
                method: 'POST',
                headers: {
                    "Content-Type": "application/json",
                    [csrfHeader]: csrfToken
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert(data.message || '판매완료 처리되었습니다.');
                    modal.hide();
                    location.reload();
                } else {
                    alert(data.message || '판매완료 처리에 실패했습니다.');
                }
            })
            .catch(err => {
                console.error(err);
                alert('판매완료 처리 중 오류가 발생했습니다.');
            });
        };
    }
}