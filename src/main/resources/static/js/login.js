const saveButton = document.getElementById('saveButton');

saveButton.addEventListener('click', async (e) => {
e.preventDefault(); // 기본 폼 제출 막기

const form = document.getElementById('loginForm');

// ✅ 부트스트랩 유효성 검사 강제 실행
if (!form.checkValidity()) {
    e.stopPropagation();
    form.classList.add('was-validated'); // 빨간 경고 표시
    return; // 유효하지 않으면 업로드/제출 중단
}

// ✅ 로그인 요청
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
            window.location.href = '/list';
        } else {
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