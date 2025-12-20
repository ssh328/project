const saveButton = document.getElementById('saveButton');

// 로그인 폼 제출 이벤트 핸들러
// 폼 유효성 검사 후 JWT 로그인 요청을 처리
saveButton.addEventListener('click', async (e) => {
    e.preventDefault(); // 기본 폼 제출 막기

    const form = document.getElementById('loginForm');

    // 부트스트랩 유효성 검사 강제 실행
    if (!form.checkValidity()) {
        e.stopPropagation();
        form.classList.add('was-validated'); // 빨간 경고 표시
        return; // 유효하지 않으면 업로드/제출 중단
    }

    // 로그인 요청
    try {
        const response = await fetch('/login/jwt', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: document.querySelector('#username').value,
                password: document.querySelector('#password').value
            })
        });

        const data = await response.json();

        if (data.success) {
            // 로그인 성공 시 게시글 목록 페이지로 이동
            window.location.href = '/post/list';
        } else {
            // 로그인 실패 시 에러 메시지 표시
            const errorDiv = document.createElement('div');
            errorDiv.className = 'alert alert-danger alert-dismissible fade show';
            errorDiv.innerHTML = `
                <strong>${data.message}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.querySelector('form').insertBefore(errorDiv, document.querySelector('form').firstChild);
        }
    } catch (err) {
        console.error(err);
        alert('서버 오류');
    }
});